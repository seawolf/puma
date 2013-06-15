package org.macno.puma.provider;

import static org.macno.puma.PumaApplication.APP_NAME;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthException;

import org.json.JSONObject;
import org.macno.puma.core.Account;
import org.macno.puma.manager.OAuthManager;
import org.macno.puma.util.HttpUtil;
import org.macno.puma.util.HttpUtilException;

import android.content.Context;
import android.util.Log;

public class Pumpio {
	
	private static final String HTTP = "http://";
	private static final String HTTPS = "https://";
	
	private static final String WHOAMI_URL = "/api/whoami";
	
	private HttpUtil mHttpUtil;
	private Context mContext;
	
	private Account mAccount;
	
	public Pumpio(Context context) {
		mContext = context;
		mHttpUtil = new HttpUtil();
	}
	
	public void setAccount(Account account) {
		mAccount = account;
		mHttpUtil.setHost(mAccount.getNode());
		OAuthManager oauthManager = new OAuthManager(mContext);
		try {
			oauthManager.prepare(mAccount.getNode());
			oauthManager.setConsumerTokenWithSecret(mAccount.getOauthToken(), mAccount.getOauthTokenSecret());
			CommonsHttpOAuthConsumer consumer = oauthManager.getConsumer();
			
			mHttpUtil.setOAuthConsumer(consumer);
		} catch (OAuthException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (HttpUtilException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public JSONObject getWhoami() {
		String url = prepareUrl(WHOAMI_URL);
		try {
			return mHttpUtil.getJsonObject(url);
		} catch (Exception e) {
			Log.e(APP_NAME, e.getMessage(), e);
		}
		return null;
	}
	
	
	private String prepareUrl(String path) {
		return mAccount.isSsl() ? HTTPS : HTTP + mAccount.getNode() + path;  
	}
}
