package com.bignerdranch.android.photogallery;

import java.util.ArrayList;


import android.annotation.TargetApi;
import android.app.Activity;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.Toast;

import com.bignerdranch.anroid.photogallery.R;

public class PhotoGalleryFragment extends VisibleFragment{
	
	/**************************************************/
	/*                 Constants                      */
	/**************************************************/
	private static final String TAG = "PhotoGalleryFragment";
	
	/**************************************************/
	/*                Local Data                      */
	/**************************************************/
	GridView mGridView;
	ArrayList<GalleryItem> mItems;
	ThumbnailDownloader<ImageView> mThumbnailThread;
	
	
	/**************************************************/
	/*               Override Methods                 */
	/**************************************************/
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//Retain fragment across Activity re-creation
		setRetainInstance(true);
		//Set Option Menu
		setHasOptionsMenu(true);
		
		//Start background thread to fetch Flickr image data
		updateItems();
		
		//Start Poll Service intent
		//Intent i = new Intent(getActivity(), PollService.class);
		//getActivity().startService(i);
		//PollService.setServiceAlarm(getActivity(), true);
		
		//Start up thread to load thumbnail images, pass main thread message 
		//Handler to it.
		Handler h = new Handler();
		mThumbnailThread = new ThumbnailDownloader<ImageView>(new Handler());
		mThumbnailThread.setListener(new Listener<ImageView>() {
		   public void onThumbnailDownloaded(ImageView imageView, Bitmap thumbnail) {
			   //Make sure fragment is visible
			   if(isVisible()) {
				   imageView.setImageBitmap(thumbnail);
			   }
		   }
		});
		//Make sure to start thread, before getting the looper
		mThumbnailThread.start();
		mThumbnailThread.getLooper();
		Log.i(TAG, "Background thread started");
		
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
		
		mGridView = (GridView)v.findViewById(R.id.gridView);
		
		//GridView is and AdapterView, so need to get an adapter
		setupAdapter();
		
		//Setup Click Listener to bring up web page to bring up flickr pic
		mGridView.setOnItemClickListener(new GridView.OnItemClickListener() {
			
			@Override
			public void onItemClick(AdapterView<?> gridView,
					View view, int pos, long id) {
			   
				GalleryItem item = mItems.get(pos);
				Uri photoPageUri = Uri.parse(item.getPhotoPageUrl());
				Intent i = new Intent(getActivity(),PhotoPageActivity.class);
				i.setData(photoPageUri);
				
				startActivity(i);
			}
		});
		
		return v;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		//Make sure to kill the tread when the fragment/activity is destoryed
		mThumbnailThread.quit();
		Log.i(TAG,"Background thread destroyed");
	}
	
	@Override
    public void onDestroyView() {
		super.onDestroyView();
        //Clean out Queue	
		mThumbnailThread.cleaQueue();
	}
	
	@TargetApi(11)
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		
		inflater.inflate(R.menu.fragment_photo_gallery, menu);
		
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			//Pull out the SearchView
			MenuItem searchItem = menu.findItem(R.id.menu_item_search);
			SearchView searchView = (SearchView)searchItem.getActionView();
	
			//Get the data form out searchable.xml as a SearchableInfo
			SearchManager searchManager = (SearchManager)getActivity()
					.getSystemService(Context.SEARCH_SERVICE);
			String query = SearchManager.QUERY;
           
			ComponentName name = getActivity().getComponentName();
			SearchableInfo searchInfo = searchManager.getSearchableInfo(name);
			
			searchView.setSearchableInfo(searchInfo);
			
		}
		
		super.onCreateOptionsMenu(menu, inflater);
		
	}
	
	@Override
	@TargetApi(11)
	public boolean onOptionsItemSelected(MenuItem item) {
		
		switch(item.getItemId()) {
		    case R.id.menu_item_search:
			    getActivity().onSearchRequested();
	
			    return true;
		    case R.id.menu_item_clear:
		    	PreferenceManager.getDefaultSharedPreferences(getActivity())
		    	.edit().putString(FlickrFetchr.PREF_SEARCH_QUERY,null).commit();
		    	
		    	//Fetch thumbnails again
		    	updateItems();
		    	return true;
		    case R.id.menu_item_toggle_polling:
		    	boolean shouldStartAlarm = !PollService.isServiceAlarmOn(getActivity());
		    	PollService.setServiceAlarm(getActivity(), shouldStartAlarm);
		    	if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
		    		getActivity().invalidateOptionsMenu();
		    	return true;
		    default:
		    	return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		
		MenuItem toggleItem = menu.findItem(R.id.menu_item_toggle_polling);
		
		if(PollService.isServiceAlarmOn(getActivity())) {
			toggleItem.setTitle(R.string.stop_polling);
		}
		else {
			toggleItem.setTitle(R.string.start_polling);
		}
	}
	
	/***************************************************/
	/*                 Private Methods                 */
	/***************************************************/
	void setupAdapter() {
		
		//Since we are using AsyncTask, we can't assume that callbacks 
		//to the fragment is attached to an Activity. So make
		//sure fragment is still attached to the activity
		Log.d(TAG,"Inside setupAdapter");
		if(getActivity() == null || mGridView == null)
			return;
		
		if(mItems != null)
		   if(mItems.size() == 0)
			   Toast.makeText(getActivity(),R.string.failed_fetch_toast, Toast.LENGTH_SHORT).show();
		
		//If there are pictures in the ArrayList, create adapter 
		//of type GalleryItemAdapter
		if(mItems != null) {
			mGridView.setAdapter(new GalleryItemAdapter(mItems));
		}
		else {			
			mGridView.setAdapter(null);
		}
	}
	
	/***************************************************/
	/*                  Public Methods                 */
	/***************************************************/
	//Start background thread to fetch Flickr image data
	public void updateItems() {
		new FetchItemsTask().execute();
	}
	/***************************************************/
	/*                Inner Class                      */
	/***************************************************/
	//Utility class to create Thread and run it in the background
	//Third parameter is the type of result produced by AsynTask. This will
	//Be the return type of doInBackground() and input type for 
	//onPostExecute()
	private class FetchItemsTask extends AsyncTask<Void,Void,ArrayList<GalleryItem>> {
	
		//Code here what to do in the background task
		@Override
		protected ArrayList<GalleryItem> doInBackground(Void... params) {
			
			Activity activity = getActivity();
			if(activity == null)
			   return new ArrayList<GalleryItem>();
			
			String query = PreferenceManager.getDefaultSharedPreferences(activity)
					.getString(FlickrFetchr.PREF_SEARCH_QUERY, null);
			
			//return list of fetched items from Flickr so it can 
			//be used by onPostExecute()
			if(query != null) {
				FlickrFetchr fetch = new FlickrFetchr();
				return fetch.search(query);
			} else {
			   FlickrFetchr fetch = new FlickrFetchr();
			   return fetch.fetchItems();
			}
			
		}
		
		//Runs after doInBackground is finished
		@Override
		protected void onPostExecute(ArrayList<GalleryItem> items) {
			
			//Set feteched items and put the data in the adapter
			
			if(items == null)
				Log.e(TAG,"Inside onPostExecute with null items. number");
			else
				Log.e(TAG,"Inside onPostExecute with success getting items number = " + String.valueOf(items.size()));
			mItems = items;
				 
			setupAdapter();
		}
	}
	
	//Need a custom ArrayAdapter to display images
	private class GalleryItemAdapter extends ArrayAdapter<GalleryItem> {
		
		//Constructor
		public GalleryItemAdapter(ArrayList<GalleryItem> items) {
			super(getActivity(), 0, items);
		}
		
		//Override the getView to return an ImageView
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
			if(convertView == null) {
				convertView = getActivity().getLayoutInflater()
						.inflate(R.layout.gallery_item, parent, false);
			}
			
			//Get imageView
			ImageView imageView = (ImageView)convertView.findViewById(R.id.gallery_item_imageView);
			
			//Set placeholder image
			imageView.setImageResource(R.drawable.brian_up_close);
			
			//Get each Gallery item using the position in the ArrayAdapter
			GalleryItem item = getItem(position);
			mThumbnailThread.queueThumbnail(imageView, item.getUrl());
			
			return convertView;
		}
	}

}
