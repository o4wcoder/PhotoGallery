package com.bignerdranch.android.photogallery;

import com.bignerdranch.android.photogallery.PhotoGalleryFragment;
import com.bignerdranch.anroid.photogallery.R;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class PhotoGalleryActivity extends SingleFragmentActivity {

	/*************************************************************/
	/*                      Constants                            */
	/*************************************************************/
	private static final String TAG = "PhotoGalleryActivity";
	
	@Override
	protected Fragment createFragment() {
		return new PhotoGalleryFragment();
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		PhotoGalleryFragment fragment = (PhotoGalleryFragment)getSupportFragmentManager()
				.findFragmentById(R.id.fragmentContainer);
		
		if(Intent.ACTION_SEARCH.equals(intent.getAction())) {
			
			String query = intent.getStringExtra(SearchManager.QUERY);
			Log.i(TAG, "Received a new search query: " + query);
			
			//Keep search query persistence. Save out search query using shared preferences
			PreferenceManager.getDefaultSharedPreferences(this).edit()
			.putString(FlickrFetchr.PREF_SEARCH_QUERY,query).commit();
		}
		
		//Update
		fragment.updateItems();
	}
}
