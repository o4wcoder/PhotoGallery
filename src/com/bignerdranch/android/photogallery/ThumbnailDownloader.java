package com.bignerdranch.android.photogallery;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.LruCache;


/*
 * Class to run as backround thread to handle loading images to 
 * UI. Need generic argument "Token" to identify each download
 */
public class ThumbnailDownloader<Token> extends HandlerThread {

	/**************************************************************/
	/*                       Constants                            */
	/**************************************************************/
	private static final String TAG = "ThumbnailDownloader";
	private static final int MESSAGE_DOWNLOAD = 0;
	
	/**************************************************************/
	/*                      Local Data                            */
	/**************************************************************/
	//Message Handler
	Handler mHandler;
	
	//Synchronized HashMap Queue. Using the Token as a key, you can store and
	//retrieve the URL associated with a particular Token
	Map<Token, String> requestMap = Collections.synchronizedMap(new HashMap<Token,String>());
	
	//Message Handler for the Main Thread
	Handler mResponseHandler;
	Listener<Token> mListener;
	
	// Get max available VM memory, exceeding this amount will throw an
    // OutOfMemory exception. Stored in kilobytes as LruCache takes an
    // int in its constructor.
    final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

	//Setup Bitmap Cache. Use 1/8th of the available 
	int cacheSize = maxMemory / 8;
	
	LruCache<String, Bitmap> bitmapCache;
	
	/**************************************************************/
	/*                      Constructors                          */
	/**************************************************************/
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
	public ThumbnailDownloader(Handler responseHandler) {
		super(TAG);
		
		//Store reference to Main Thread Handler
		mResponseHandler = responseHandler;
		
		//Create cache
		bitmapCache = new LruCache<String, Bitmap>(cacheSize) {
			@Override
			protected int sizeOf(String key, Bitmap bitmap) {
				return bitmap.getByteCount() / 1024;
			}
		};
	}
	
	
	/**************************************************************/
	/*                     Override Methods                       */
	/**************************************************************/
	/*
	 * Warning from lint given because Handler is anonymous inner 
	 * class. However, everything is tied to HandlerThread so 
	 * no danger of memory leak
	 */
	@SuppressLint("HandlerLeak")
	@Override
	/* Function to before tasks before the looper starts */
	protected void onLooperPrepared() {
		
		/* onLooperPrepared is called before Looper check the queue
		 * for the first time. Good place to create the Handler
		 */
		mHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				//check message type
				if(msg.what == MESSAGE_DOWNLOAD) {
					//suppress warning about type erasure
					@SuppressWarnings("unchecked")
					Token token = (Token)msg.obj;
					//Log.i(TAG, "Got a request for url: " +
					//requestMap.get(token));
					handleRequest(token);
					
				}
			}
		};
	}
	
	/**************************************************************/
	/*                      Private Methods                       */
	/**************************************************************/
	/* Download the thumbnail images upon request */
	
	private void handleRequest(final Token token) {
		
		final Bitmap bitmap;
		
		try {
			
			//check if the URL exists in the queue
			final String url = requestMap.get(token);
			if(url == null)
				return;
			
			if(getBitmapFromCache(url) == null) {
			   //Download bytes from the URL and turn the bytes into a bitmap
			   byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
			   bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
			
			   //Add Bitmap to cache
			   addBitmapToCache(url, bitmap);
			}
			else {
				bitmap = getBitmapFromCache(url);
			}
			
			//Run code on main thread
			mResponseHandler.post(new Runnable() {
				public void run() {
					
					//GridView recycles views, so make sure that the 
					//token has the correct URL
					if(requestMap.get(token) != url)
						return;
					
					//Remove token from request map queue
					requestMap.remove(token);
					//Set the Bitmap on the token (ImageView)
					mListener.onThumbnailDownloaded(token, bitmap);
				}
			});
			
		} catch(IOException ioe) {
			Log.e(TAG, "Error downloading image", ioe);
		}
	}
	/**************************************************************/
	/*                      Public Methods                        */
	/**************************************************************/
	public void queueThumbnail(Token token, String url) {
		
		//Log.i(TAG, "Got an URL: " + url);
		
		//Add Token URL to the Map Queue
		requestMap.put(token, url);
		
		//Obtain a message, give it the token as it's obj, and send it
		//off to be put on the message queue
		mHandler.obtainMessage(MESSAGE_DOWNLOAD,token).sendToTarget();
		
	
	}
	
	public void setListener(Listener<Token> listener) {
		mListener = listener;
	}
	
	//Clear all requests out of the queue
	public void cleaQueue() {
		mHandler.removeMessages(MESSAGE_DOWNLOAD);
		requestMap.clear();
	}
	
	@TargetApi(12)
	public void addBitmapToCache(String key, Bitmap bitmap) {
		
		 if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
		    if(getBitmapFromCache(key) == null)
		    {
			   bitmapCache.put(key, bitmap);
		    }
		 }
	}
	@TargetApi(12)
	public Bitmap getBitmapFromCache(String key) {
		
		 if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) 
	        return bitmapCache.get(key);
	     else
	    	 return null;
	}
	
	
	
}
