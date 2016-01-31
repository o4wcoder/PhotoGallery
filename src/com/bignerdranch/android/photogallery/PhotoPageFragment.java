package com.bignerdranch.android.photogallery;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bignerdranch.anroid.photogallery.R;

public class PhotoPageFragment extends VisibleFragment {
	
	/********************************************************/
	/*                     Loca Data                        */
	/********************************************************/
	private String mUrl;
	private WebView mWebView;
	
	/********************************************************/
	/*                   Override Methods                   */
	/********************************************************/
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		
		mUrl = getActivity().getIntent().getData().toString();
	}
	
	public View onCreateView(LayoutInflater inflater, ViewGroup parent,
			Bundle savedInstanceState) {
		
		View v = inflater.inflate(R.layout.fragment_photo_page, parent, false);
		
		final ProgressBar progressBar = (ProgressBar)v.findViewById(R.id.progressBar);
		progressBar.setMax(100); //WebChromeClient reports in range 0 - 100
		
		final TextView titleTextView = (TextView)v.findViewById(R.id.titleTextView);
		
		mWebView = (WebView)v.findViewById(R.id.webView);
		
		//Turn on Javascript by getting an instance of WebSettings
		mWebView.getSettings().setJavaScriptEnabled(true);
		
		//Override what to do when a new URL is loaded in WebView, like pressing a link
		mWebView.setWebViewClient(new WebViewClient() {
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				//Return false to tell it to load the URL in the WebView. If set to true,
				//must write got to handle it and it will go to the browser or
				//have other choices to pick from to display it.
				return false;
			}
		});
		
		mWebView.setWebChromeClient(new WebChromeClient() {
			public void onProgressChanged(WebView webView, int progress) {
				//Progress is from 0 -100. When it hits 100, it's done loading
				if(progress == 100) {
					progressBar.setVisibility(View.INVISIBLE);
				}
				else {
					progressBar.setVisibility(View.VISIBLE);
					progressBar.setProgress(progress);
				}
			}
			
			public void onReceivedTitle(WebView webView, String title) {
				titleTextView.setText(title);
			}
		});
		
		mWebView.loadUrl(mUrl);
		
		return v;
		
	}

}
