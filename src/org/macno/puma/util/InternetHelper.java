package org.macno.puma.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Class that provides functions for checking if internet access is
 * available, and if so the type of access (WiFi of mobile).
 *
 */
public class InternetHelper {
	private Context mCtx;
	private ConnectivityManager mConnectivity;
	private NetworkInfo mInfo;
	
	public InternetHelper(Context ctx){
		mCtx = ctx;;
		mConnectivity =  (ConnectivityManager) mCtx.getSystemService(
				Context.CONNECTIVITY_SERVICE);
	}
	
	/**
	 * Detect whether the phone has an internet connection.
	 * @return True if the phone can connect to the internet, false if not.
	 */
	public final boolean checkConnectivity(){
		mInfo = mConnectivity.getActiveNetworkInfo();
		if (mInfo == null){
			 return false;
		} else return true;	
	}
	
	/** Get the type of network connection currently active.
	 * @return Either TYPE_WIFI or TYPE_MOBILE, or -1 if no connection.
	 */
	public final int getConnectivityType(){
		mInfo = mConnectivity.getActiveNetworkInfo();
		if (mInfo != null){
			return mInfo.getType();
		} else {
			return -1;
		}
	}
}
