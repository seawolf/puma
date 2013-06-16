package org.macno.puma.manager;

import static org.macno.puma.PumaApplication.APP_NAME;

import android.content.Context;
import android.util.Log;

public class AppManager {

	private Context mContext;
	
	public AppManager(Context context) {
		mContext = context;
	}
	
	public boolean doUpgrade(int from, int to) {
		Log.d(APP_NAME,"Upgrading app from " + from + " to " + to);

		if(from == 0) {
			// Clear appsettings
			OAuthManager om = new OAuthManager(mContext);
			om.removeConsumersKey();
			
		}
		return true;
	}
	
	public boolean doDowngrade(int from, int to) {
		
		
		return true;
	}
	
}
