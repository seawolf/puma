package org.macno.puma.util;

import static org.macno.puma.PumaApplication.APP_NAME;

import java.text.ParseException;

import org.json.JSONObject;
import org.macno.puma.R;
import org.macno.puma.view.RemoteImageView;

import android.content.Context;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

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
	
	public static LinearLayout getViewActivity(Context mContext,JSONObject act) {
		
		LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		LinearLayout view = (LinearLayout)inflater.inflate(R.layout.activity_row, null);

		JSONObject obj = act.optJSONObject("object");
		JSONObject actor = ActivityUtil.getActor(act);
		String objectType = obj.optString("objectType");
		TextView sender = (TextView)view.findViewById(R.id.tv_sender);
		TextView note = (TextView)view.findViewById(R.id.note);
		if("post".equals(act.optString("verb"))) {
			String message="";
			String what = "";
			if(objectType.equals("note")) {
				what = mContext.getString(R.string.objecttype_note);
			} else if(objectType.equals("comment")) {
				what = mContext.getString(R.string.objecttype_comment);
			} else if(objectType.equals("image")) {
				what = mContext.getString(R.string.objecttype_image);
			} else {
				what = "something";
			}
			if("note".equals(objectType)) {
				try {
					String content = ActivityUtil.getContent(obj);
					if(content != null)
						note.setText(Html.fromHtml(content));
				} catch(Exception e) {
					Log.e(APP_NAME,"Setting note html: " + e.getMessage(),e);
					note.setText(ActivityUtil.getContent(obj));
				}
			} else if("comment".equals(objectType)) {
				note.setText(Html.fromHtml(ActivityUtil.getContent(obj)));
			} else if("image".equals(objectType)) {
				String content = ActivityUtil.getContent(obj);
				if(content != null)
					note.setText(Html.fromHtml(content));
				RemoteImageView noteImage = (RemoteImageView)view.findViewById(R.id.note_image);
				noteImage.setVisibility(View.VISIBLE);
				noteImage.setRemoteURI(ActivityUtil.getObjectImage(obj));
				noteImage.loadImage();
			}
			message = mContext.getString(R.string.msg_posted,ActivityUtil.getActorBestName(actor), what);
			sender.setText(message);
		} else if ("favorite".equals(act.optString("verb"))) {
			String message="";
			String what = "";
			if(objectType.equals("note")) {
				what = mContext.getString(R.string.objecttype_note);
			} else if(objectType.equals("comment")) {
				what = mContext.getString(R.string.objecttype_comment);
			} else if(objectType.equals("image")) {
				what = mContext.getString(R.string.objecttype_image);
			} else {
				what = "something";
			}
			message = mContext.getString(R.string.msg_favorited,ActivityUtil.getActorBestName(actor),  what);
			sender.setText(message);
			note.setText(Html.fromHtml(ActivityUtil.getContent(obj)));
		} else if ("share".equals(act.optString("verb"))) {
			String message="";
			String what = "";
			if(objectType.equals("note")) {
				what = mContext.getString(R.string.objecttype_note);
			} else if(objectType.equals("comment")) {
				what = mContext.getString(R.string.objecttype_comment);
			} else if(objectType.equals("image")) {
				what = mContext.getString(R.string.objecttype_image);
			} else {
				what = "something";
			}
			JSONObject originalActor = ActivityUtil.getActor(obj);
			if(originalActor != null) {
				message = mContext.getString(R.string.msg_shareded_from,ActivityUtil.getActorBestName(actor), what, ActivityUtil.getActorBestName(originalActor));
			} else {
				message = mContext.getString(R.string.msg_shareded,ActivityUtil.getActorBestName(actor), what);
			}
			sender.setText(message);
			note.setText(Html.fromHtml(ActivityUtil.getContent(obj)));
		}

		
		RemoteImageView rim = (RemoteImageView)view.findViewById(R.id.riv_sender);
		String avatar = ActivityUtil.getImageUrl(actor);
		if(avatar == null) {
			avatar = "http://macno.org/images/unkown.png";
		}
		rim.setRemoteURI(avatar);
		rim.loadImage();
		
		TextView published = (TextView)view.findViewById(R.id.tv_published);
		String s_published = ActivityUtil.getPublished(act);
		try {
			s_published = DateUtils.getRelativeDate(mContext, 
					DateUtils.parseRFC3339Date(s_published)
					);
		} catch (ParseException e) {
			Log.e(APP_NAME,e.getMessage(),e);
		}
		published.setText(s_published);
		
		return view;
		
	}
	
}
