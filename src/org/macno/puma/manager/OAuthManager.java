package org.macno.puma.manager;


import static org.macno.puma.PumaApplication.APP_NAME;
import static org.macno.puma.PumaApplication.K_OAUTH_SETTINGS;

import java.util.ArrayList;

import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
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
import android.util.Log;

public class OAuthManager {

	private static final String REQUEST_TOKEN_URL = "https://%s/oauth/request_token";
	private static final String AUTHORIZE_URL     = "https://%s/oauth/authorize";
	private static final String ACCESS_TOKEN_URL = "https://%s/oauth/access_token";

	private static final String REGISTER_CLIENT_URL = "%s/api/client/register";

	private SharedPreferences mSettings;
	private Context mContext;

	private CommonsHttpOAuthConsumer	mConsumer;
	private CommonsHttpOAuthProvider 	mProvider;
	
	public OAuthManager(Context context) {
		mContext = context;
		mSettings = context.getSharedPreferences(K_OAUTH_SETTINGS,Context.MODE_PRIVATE);
	}
	
	public void prepareConsumerForHost(String host) throws HttpUtilException, OAuthException {
		
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
			String baseUrl = String.format(REGISTER_CLIENT_URL, host);
			
			JSONObject json = null;
			try {
				// First try in https
				json = httpUtil.getJsonObject("https://"+baseUrl, HttpUtil.POST, params);
				
			} catch (HttpUtilException e) {
				Log.e(APP_NAME, "https://"+baseUrl);
				e.printStackTrace();
				throw e;
				// Do another try using http
//				try {
//					json = httpUtil.getJsonObject("http://"+baseUrl, HttpUtil.POST, params);
//				} catch (HttpUtilException e2) {
//					e2.printStackTrace();
//					throw e2;
//				}
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
		mConsumer = new CommonsHttpOAuthConsumer(clientId,clientSecret);
	}

	public void removeConsumerKeyForHost(String host) {
		mSettings.edit().remove(host).remove(host+":secret").commit();
	}
	
	public void removeConsumersKey() {
		mSettings.edit().clear().commit();
	}
	public void prepare(String clientId, String clientSecret,String host) throws OAuthException , HttpUtilException {

		if(clientId==null && clientSecret == null) {
			prepareConsumerForHost(host);
		} else {
			mConsumer = new CommonsHttpOAuthConsumer(clientId,clientSecret);
		}
		prepareProviderForHost(host);
		
	}
	
	
	public void prepareProviderForHost(String host) {
		mProvider = new CommonsHttpOAuthProvider(
				String.format(REQUEST_TOKEN_URL, host), 
				String.format(ACCESS_TOKEN_URL, host),
				String.format(AUTHORIZE_URL, host)
				);
		mProvider.setOAuth10a(true);
	}
	
	public void setConsumerTokenWithSecret(String token, String secret) {
		mConsumer.setTokenWithSecret(token, secret);
		
	}
	
	public String retrieveRequestToken(String callbackUrl) throws OAuthException {
		if (mProvider == null || mConsumer == null) {
			Log.e(APP_NAME, "retrivedRequestToken called *before* prepare!");
			return null;
		}
		return mProvider.retrieveRequestToken(mConsumer,callbackUrl);
		
	}

	public void retrieveAccessToken(String verifier) throws OAuthException {
		if (mProvider == null || mConsumer == null) {
			Log.e(APP_NAME, "retrieveAccessToken called *before* prepare!");
			return;
		}
		mProvider.retrieveAccessToken(mConsumer, verifier);
	}

	public CommonsHttpOAuthConsumer getConsumer() {
    	if (mConsumer == null) {
			Log.e(APP_NAME, "getConsumer called *before* prepare!");
			return null;
		}
    	return mConsumer;
    }
}
