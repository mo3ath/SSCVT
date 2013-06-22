package com.sscvt.helper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

/**
*
* This BroadcastReceiver intercepts the android.net.ConnectivityManager.CONNECTIVITY_ACTION,
* which indicates a connection change. It checks whether the type is TYPE_WIFI.
* If it is, it checks whether Wi-Fi is connected and sets the wifiConnected flag in the
* main activity accordingly.
*
*/
public class NetworkReceiver extends BroadcastReceiver {
    // Whether there is a Wi-Fi connection.
    public static boolean wifiConnected = false;
    // Whether there is a mobile connection.
    public static boolean mobileConnected = false;
    private Context mContext = null;
    
    public NetworkReceiver(Context context){
    	mContext = context;
    	Log.d("Network", "NetworkReceiver constructor");
    }

   @Override
   public void onReceive(Context context, Intent intent) {
	   Log.d("Network", "onReceive");
	   updateConnectedFlags();
   }
   
// Checks the network connection and sets the wifiConnected and mobileConnected
   // variables accordingly.
   public void updateConnectedFlags() {
	   Log.d("Network", "updating");
	   ConnectivityManager connMgr =
               (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
       NetworkInfo activeInfo = connMgr.getActiveNetworkInfo();
       if (activeInfo != null && activeInfo.isConnected()) {
           wifiConnected = activeInfo.getType() == ConnectivityManager.TYPE_WIFI;
           mobileConnected = activeInfo.getType() == ConnectivityManager.TYPE_MOBILE;
       } else {
           wifiConnected = false;
           mobileConnected = false;
       }
   }
   
   // return true if network is connected
   public boolean isConnected(){
	   return (wifiConnected || mobileConnected);
   }
}