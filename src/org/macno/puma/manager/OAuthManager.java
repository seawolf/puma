package org.macno.puma.manager;


import static org.macno.puma.PumaApplication.K_OAUTH_SETTINGS;

import java.util.ArrayList;

import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthException;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.macno.puma.R;
import org.macno.puma.util.HttpUtil;
import org.macno.puma.util.HttpUtilException;

import android.content.Context;
import android.content.SharedPreferences;

public class OAuthManager {

	private static final String K_REGISTER_CLIENT_URL = "%s/api/client/register";

	private SharedPreferences mSettings;
	private Context mContext;
	
	public OAuthManager(Context context) {
		mContext = context;
		mSettings = context.getSharedPreferences(K_OAUTH_SETTINGS,Context.MODE_PRIVATE);
	}
	
	public CommonsHttpOAuthConsumer getConsumerForHost(String host) throws HttpUtilException, OAuthException {
		
		String clientId = mSettings.getString(host, null);
		String clientSecret = null;
		if(clientId != null) {
			clientSecret = mSettings.getString(host + ":secret", null);
		} else {

			String appName = mContext.getString(R.string.app_name);
			
			HttpUtil httpUtil = new HttpUtil(host);

			
			ArrayList<NameValuePair> params = new ArrayList<NameValuePair>(); 
			params.add(new BasicNameValuePair("type", "client_associate"));
			params.add(new BasicNameValuePair("application_type", "native"));
			params.add(new BasicNameValuePair("client_name", appName));
			params.add(new BasicNameValuePair("application_name", appName));
			String baseUrl = String.format(K_REGISTER_CLIENT_URL, host);
			
			JSONObject json = null;
			try {
				// First try in https
				json = httpUtil.getJsonObject("https://"+baseUrl, HttpUtil.POST, params);
				
			} catch (HttpUtilException e) {
				e.printStackTrace();
				// Do another try using http
				try {
					json = httpUtil.getJsonObject("http://"+baseUrl, HttpUtil.POST, params);
				} catch (HttpUtilException e2) {
					e2.printStackTrace();
					throw e2;
				}
			}
			
			try {
				clientId = json.getString("client_id");
				clientSecret = json.getString("client_secret");
				mSettings
					.edit()
						.putString(host, clientId)
						.putString(host + ":secret", clientSecret)
							.commit();
			} catch (JSONException e) {
				e.printStackTrace();
			}
						
		}
		return new CommonsHttpOAuthConsumer(clientId,clientSecret);
	}

	public void removeConsumerForHost(String host) {
		mSettings.edit().remove(host).remove(host+":secret").commit();
	}
	

}
