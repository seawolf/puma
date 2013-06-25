package org.macno.puma.manager;

import static org.macno.puma.PumaApplication.K_SSLHOSTS_SETTINGS;
import android.content.Context;
import android.content.SharedPreferences;

public class SSLHostTrustManager {

	private SharedPreferences mSettings;
	
	public SSLHostTrustManager(Context context) {
		mSettings = context.getSharedPreferences(K_SSLHOSTS_SETTINGS,Context.MODE_PRIVATE);
	}
	
	public void addHost(String host) {
		mSettings.edit().putBoolean(host, true).commit();
	}
	
	public boolean hasHost(String host) {
		return mSettings.getBoolean(host, false);
	}
	
}
