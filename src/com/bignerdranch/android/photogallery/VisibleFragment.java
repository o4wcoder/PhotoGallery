package com.bignerdranch.android.photogallery;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.widget.Toast;

//Generic Fragment that hides forground Notifications
public abstract class VisibleFragment extends Fragment {

	/*********************************************************/
	/*                     Constants                         */
	/*********************************************************/
	public static final String TAG = "VisibleFragment";
	
	private BroadcastReceiver mOnShowNotification = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			//Toast.makeText(getActivity(), "Got a broadcast:" + intent.getAction(),
				//	Toast.LENGTH_LONG).show();
			//If we receive this, we're visible, so cancel the notification
			Log.i(TAG,"canceling notification");
			setResultCode(Activity.RESULT_CANCELED);
		}
	};
	
	@Override
	public void onResume() {
		super.onResume();
		//Create notification intent filter and register the receiver. Set private
		//permission so only this app can trigger the receiver
		IntentFilter filter = new IntentFilter(PollService.ACTION_SHOW_NOTIFICATION);
		getActivity().registerReceiver(mOnShowNotification, filter,
				PollService.PERM_PRIVATE, null);
		
	}
	
	@Override
	public void onPause() {
		super.onPause();
		//Unregister the receiver
		getActivity().unregisterReceiver(mOnShowNotification);
	}
}
