package org.macno.puma.manager;


import static org.macno.puma.PumaApplication.K_OAUTH_SETTINGS;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;

public class OAuthManager {


	private SharedPreferences mSettings;
	
	public OAuthManager(Context context) {
		mSettings = context.getSharedPreferences(K_OAUTH_SETTINGS,Context.MODE_PRIVATE);
	}
	
	
	public OAuthConsumer getConsumerForHost(String host) {
		
		String clientId = mSettings.getString(host, null);
		String clientSecret = null;
		if(clientId != null) {
			clientSecret = mSettings.getString(host + ":secret", null);
		} else {

			String response = "";
			

			try {
				JSONObject json = new JSONObject(response);
				clientId = json.getString("client_id");
				clientSecret = json.getString("client_secret");
			} catch (JSONException e) {
				e.printStackTrace();
			}

		}
		return new DefaultOAuthConsumer(clientId,clientSecret);
	}
	
}
