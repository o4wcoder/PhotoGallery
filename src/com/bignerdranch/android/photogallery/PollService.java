package com.bignerdranch.android.photogallery;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.bignerdranch.anroid.photogallery.R;

public class PollService extends IntentService {

	/*********************************************************/
	/*                    Constants                          */
	private static final String TAG = "PollService";
	//private static final int POLL_INTERVAL = 1000 * 60 * 5; // 5 minutes
	private static final int POLL_INTERVAL = 1000 * 15; //15 seconds
			
	public static final String PREF_IS_ALARM_ON = "isAlarmOn";
	public static final String ACTION_SHOW_NOTIFICATION = 
			"com.bignerdranch.android.photogallery.SHOW_NOTIFICATION";
	public static final String PERM_PRIVATE = 
			"com.bignerdranch.android.photogallery.PRIVATE";
	
	/*********************************************************/
	/*                   Constructors                        */
	/*********************************************************/
	public PollService() {
		super(TAG);
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		
		//See if network is available
		@SuppressWarnings("deprecation")
		boolean isNetworkAvailable = cm.getBackgroundDataSetting() &&
		cm.getActiveNetworkInfo() != null;
		
		if(!isNetworkAvailable)
			return;
		
		//Pull out the current query and the last result ID
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String query = prefs.getString(FlickrFetchr.PREF_SEARCH_QUERY, null);
		String lastResultId = prefs.getString(FlickrFetchr.PREF_LAST_RESULT_ID, null);
		
		ArrayList<GalleryItem> items;
		if(query != null) {
			items = new FlickrFetchr().search(query);
		}
		else {
			items = new FlickrFetchr().fetchItems();
		}
		
		if(items.size() == 0)
			return;
		
		//If there were results, get the first one
		String resultId = items.get(0).getId();
		
		//Check to see if it is different from the last one
		if(!resultId.equals(lastResultId)) {
			Log.i(TAG, "Got a new result: " + resultId);
			Resources r = getResources();
			
			//Pending intent will be fired when notificaiton is pressed
			Intent i = new Intent(this,PhotoGalleryActivity.class);
			PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);
			
			//Configure the ticker text
			Notification notification = new NotificationCompat.Builder(this)
			.setTicker(r.getString(R.string.new_pictures_title))
			.setSmallIcon(android.R.drawable.ic_menu_report_image)
			.setContentTitle(r.getString(R.string.new_pictures_title))
			.setContentText(r.getString(R.string.new_pictures_text))
			.setContentIntent(pi)
			.setAutoCancel(true)
			.build();
			
			
			/*
			NotificationManager notificationManager = (NotificationManager)
					getSystemService(NOTIFICATION_SERVICE);
			
			notificationManager.notify(0,notification);
			
			//Broadcast Notification. Set private permission so that
			//any application must use that same permission to receive the intent
			sendBroadcast(new Intent(ACTION_SHOW_NOTIFICATION), PERM_PRIVATE);
			*/
			showBackgroundNotification(0,notification);
		}

		
		prefs.edit().putString(FlickrFetchr.PREF_LAST_RESULT_ID, resultId).commit();
		
        Log.i(TAG,"Received an intent: " + intent);
	}
	
	/*********************************************************************/
	/*                           Private Methods                         */
	/*********************************************************************/
	//Send and ordered broadcast
	void showBackgroundNotification(int requestCode, Notification notification) {
		//Create intent notification and package it up
		Intent i = new Intent(ACTION_SHOW_NOTIFICATION);
		
		i.putExtra("REQUEST_CODE", requestCode);
		i.putExtra("NOTIFICATION", notification);
		
		//Send the notification out as a broadcast
		sendOrderedBroadcast(i,PERM_PRIVATE, null, null, Activity.RESULT_OK, null, null);
		
		
	}
	/*********************************************************************/
	/*                          Public Methods                           */
	/*********************************************************************/
	public static void setServiceAlarm(Context context, boolean isOn) {
		
		//Construct pending intent that will start PollService
		Intent i = new Intent(context, PollService.class);
		PendingIntent pi = PendingIntent.getService(context, 0, i, 0);
		
		//Set up alarm
		AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		
		if(isOn) {
			//Start the alarm. Fire the Pending Intent "pi" when the alarm goes off
			alarmManager.setRepeating(AlarmManager.RTC, System.currentTimeMillis(), POLL_INTERVAL, pi);
		}
		else {
			//Cancel the alarm
			alarmManager.cancel(pi);
			pi.cancel();
		}
		
		//Store if alarm is on or off so StartupReceiver can use it to turn
		//it on at bootup
		PreferenceManager.getDefaultSharedPreferences(context)
		.edit().putBoolean(PollService.PREF_IS_ALARM_ON, isOn).commit();
	}
	
	//See if the alarm in on or not
	public static boolean isServiceAlarmOn(Context context) {
		Intent i = new Intent(context, PollService.class);
		
		//Use "FLAG_NO_CREATE" to just tell if the alarm is
		//on or not and don't start the PendingIntent
		PendingIntent pi = PendingIntent.getService(context, 0, i,
				PendingIntent.FLAG_NO_CREATE);
		
		//Null pending intent means that the alarm is not set
		return pi != null;
		
		
	}

}
