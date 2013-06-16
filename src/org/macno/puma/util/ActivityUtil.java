package org.macno.puma.util;

import static org.macno.puma.PumaApplication.APP_NAME;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class ActivityUtil {

	public static JSONObject getActor(JSONObject act) {
		JSONObject ret = act.optJSONObject("actor");
		return ret;
	}
	
	public static String getActorBestName(JSONObject actor) {
		
		if(actor.has("displayName")) {
			return actor.optString("displayName");
		} else if(actor.has("preferredUsername")) {
			return actor.optString("preferredUsername");
		}
		return null;
	}
	
	public static String getImageUrl(JSONObject image) {
		
		if(image.has("image")) {
			return image.optJSONObject("image").optString("url");
		}
		
		return null;
	}
	
	public static String getPublished(JSONObject act) {
		return act.optString("published",null);
	}
	
	public static String getContent(JSONObject act) {
		return act.optString("content",null);
	}
	
	public static String getObjectImage(JSONObject obj) {
		return obj.optJSONObject("image").optString("url");
	}
}
