package org.macno.puma.provider;

import static org.macno.puma.PumaApplication.APP_NAME;

import java.util.ArrayList;

import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthException;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.macno.puma.core.Account;
import org.macno.puma.manager.OAuthManager;
import org.macno.puma.util.HttpUtil;
import org.macno.puma.util.HttpUtilException;

import android.content.Context;
import android.location.Location;
import android.util.Log;

public class Pumpio {
	
	private static final String HTTP = "http://";
	private static final String HTTPS = "https://";
	
	private static final String WHOAMI_URL = "/api/whoami";
	private static final String POST_NOTE_URL = "/api/user/%s/feed";
	
	private static final String ACTIVITY_STREAM_URL = "/api/user/%1$s/%2$s";
	
	private boolean mDebug = false;
	
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
			oauthManager.prepare(mAccount.getOauthClientId(),mAccount.getOauthClientSecret(), mAccount.getNode());
			oauthManager.setConsumerTokenWithSecret(mAccount.getOauthToken(), mAccount.getOauthTokenSecret());
			
			CommonsHttpOAuthConsumer consumer = oauthManager.getConsumer();
			
//			Log.d(APP_NAME,consumer.getConsumerKey()+"\n"
//					+consumer.getConsumerSecret()+"\n"
//					+consumer.getToken()+"\n"
//					+consumer.getTokenSecret());
			
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
	
	public JSONObject fetchStream(String feed, String since, String before, int count) {
		JSONObject ret = null;
		String url = null;
		if(feed.startsWith("http://") || feed.startsWith("https://")) {
			url = feed;
		} else {
			url = prepareUrl(String.format(ACTIVITY_STREAM_URL, mAccount.getUsername(),feed));
		}
		try {
			ArrayList<NameValuePair> params = new ArrayList<NameValuePair>(); 
			params.add(new BasicNameValuePair("count", ""+count));
			if(since != null) {
				params.add(new BasicNameValuePair("since", since));
			} else if(before != null) {
				params.add(new BasicNameValuePair("before", before));
			}
			ret = mHttpUtil.getJsonObject(url, HttpUtil.GET, params);
			
		} catch (HttpUtilException e) {
			Log.e(APP_NAME,e.getMessage(),e);
		}
		return ret;
	}
	
	public boolean postNote(JSONObject inReplyTo, String note, boolean publicNote, Location location) {
		
		JSONObject obj = new JSONObject();
		JSONObject act = new JSONObject();
		
		try {
			if(inReplyTo == null) {
				obj.put("objectType", "note");
			} else {
				obj.put("objectType", "comment");
				obj.put("inReplyTo", inReplyTo);
			}
			
			obj.put("content", note);

			act.put("generator", getGenerator());
			act.put("verb", "post");
			act.put("object", obj);
			if(publicNote) {
				
				JSONArray tos = new JSONArray();
				JSONObject to = new JSONObject();
				to.put("objectType", "collection");
				to.put("id", "http://activityschema.org/collection/public");
				tos.put(to);
				act.put("to", tos);
			}
			
			if(location != null) {
				
				JSONObject loc = new JSONObject();
				
				JSONObject position = new JSONObject();
				position.put("altitude", location.getAltitude());
				position.put("latitude", location.getLatitude());
				position.put("longitude", location.getLongitude());
				loc.put("position", position);
				act.put("location", loc);
			}
		} catch(JSONException e) {
			Log.e(APP_NAME,e.getMessage(),e);
		}
		String url = prepareUrl(String.format(POST_NOTE_URL, mAccount.getUsername()));
		mHttpUtil.setContentType("application/json");
		try {
			if(mDebug)
				Log.d(APP_NAME, act.toString(3));
			JSONObject ret = mHttpUtil.getJsonObject(url, HttpUtil.POST, act.toString());
			if(mDebug)
				Log.d(APP_NAME, ret.toString(3));
			return true;
		} catch(HttpUtilException e) {
			Log.e(APP_NAME,e.getMessage(),e);
			return false;
		}  catch(JSONException e) {
			Log.e(APP_NAME,e.getMessage(),e);
			return false;
		}
	}
	
	public boolean shareNote(JSONObject obj) {
		
		JSONObject act = new JSONObject();
		
		try {
			
			act.put("generator", getGenerator());
			act.put("verb", "share");
			act.put("object", obj);
			
		} catch(JSONException e) {
			Log.e(APP_NAME,e.getMessage(),e);
		}
		String url = prepareUrl(String.format(POST_NOTE_URL, mAccount.getUsername()));
		mHttpUtil.setContentType("application/json");
		try {
			if(mDebug)
				Log.d(APP_NAME, act.toString(3));
			JSONObject ret = mHttpUtil.getJsonObject(url, HttpUtil.POST, act.toString());
			if(mDebug)
				Log.d(APP_NAME, ret.toString(3));
			return true;
		} catch(HttpUtilException e) {
			Log.e(APP_NAME,e.getMessage(),e);
			return false;
		}  catch(JSONException e) {
			Log.e(APP_NAME,e.getMessage(),e);
			return false;
		}
	}
	
	private String prepareUrl(String path) {
		return (mAccount.isSsl() ? HTTPS : HTTP) + mAccount.getNode() + path;  
	}
	
	private JSONObject getGenerator() {
		JSONObject generator = new JSONObject();
		JSONObject appImage = new JSONObject();
		try {
			appImage.put("url", "http://gitorious.org/puma-droid/puma/blobs/raw/master/res/drawable-xxhdpi/puma_logo.jpg");

			generator
			.put("summary", "<a href='http://gitorious.org/puma-droid'>Puma</a>  is a pump.io client for android")
			.put("displayName", "Puma")
			.put("id","org.macno.puma")
			.put("objectType","application")
			.put("url", "http://gitorious.org/puma-droid")
			.put("image", appImage);
		} catch(JSONException e) {
			Log.e(APP_NAME,e.getMessage(),e);
		}
		return generator;
	}
}
