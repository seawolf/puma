package org.macno.puma;

import org.macno.puma.manager.AppManager;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

public class PumaApplication extends Application {

	public static final String APP_NAME = "Puma";
	
	public static final String K_SSLHOSTS_SETTINGS = "SSLHostsSettings";
	public static final String K_ACCOUNT_SETTINGS = "AccountsSettings";
	public static final String K_OAUTH_SETTINGS = "OAuthSettings";
	public static final String K_PUMA_SETTINGS = "PumaSettings";

	public static final int K_MAX_CACHED_ITEMS = 100;

	private static final String K_VERSION = "pumaVersion";
	
	private SharedPreferences mSettings;
	
	@Override
	public void onCreate() {
		super.onCreate();
		mSettings = getSharedPreferences(K_PUMA_SETTINGS,Context.MODE_PRIVATE);
		doVersionCheck();
	}
	
	public int getVersionCode() {
		try {
			PackageManager manager = getPackageManager();
			PackageInfo info = manager.getPackageInfo(getPackageName(), 0);
			return info.versionCode;
		} catch (NameNotFoundException e) {
			return 1;
		}
	}
	
	private void doVersionCheck() {
		int previousVersion = mSettings.getInt(K_VERSION, 0);
		int currentVersion = getVersionCode();
		if(previousVersion != currentVersion) {
			AppManager appManager = new AppManager(this);
			if(previousVersion < currentVersion) {
				appManager.doUpgrade(previousVersion, currentVersion);
			} else {
				appManager.doDowngrade(previousVersion, currentVersion);
			}
			mSettings.edit().putInt(K_VERSION, currentVersion).commit();
		}
	}

}
