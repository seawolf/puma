package org.macno.puma.adapter;

import static org.macno.puma.PumaApplication.APP_NAME;

import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.macno.puma.R;
import org.macno.puma.core.Account;
import org.macno.puma.provider.Pumpio;
import org.macno.puma.util.ActivityUtil;
import org.macno.puma.util.DateUtils;
import org.macno.puma.view.RemoteImageView;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ActivityAdapter extends ArrayAdapter<JSONObject> {

	private String mFeed;
	private Context mContext;
	private Account mAccount;
	private StreamHandler mHandler = new StreamHandler(this);

	private JSONArray mItems;
	
	public ActivityAdapter(Context context, Account account, String feed) {
		super(context,0,new ArrayList<JSONObject>());
		mContext = context;
		mAccount = account;
		mFeed = feed;
		loadStreams();
	}
	
	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		LinearLayout view;
		JSONObject act  = getItem(position);
			
		LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		view = (LinearLayout)inflater.inflate(R.layout.activity_row, null);
			
		Log.d(APP_NAME,"Act " + position + " item " + act.optString("id") + " <<<");
		JSONObject obj = act.optJSONObject("object");
		String objectType = obj.optString("objectType");
		TextView note = (TextView)view.findViewById(R.id.note);
		if(objectType == null ) {
			note.setText(Html.fromHtml(ActivityUtil.getContent(act)));
		} else if("note".equals(objectType)) {
			
			note.setText(Html.fromHtml(ActivityUtil.getContent(obj)));
		} else if("image".equals(objectType)) {
			note.setText(Html.fromHtml(ActivityUtil.getContent(obj)));
			RemoteImageView noteImage = (RemoteImageView)view.findViewById(R.id.note_image);
			noteImage.setVisibility(View.VISIBLE);
			noteImage.setRemoteURI(ActivityUtil.getObjectImage(obj));
			noteImage.loadImage();
		}
		JSONObject actor = ActivityUtil.getActor(act);
		TextView sender = (TextView)view.findViewById(R.id.tv_sender);
		sender.setText(ActivityUtil.getActorBestName(actor));
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
		
		view.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				JSONObject act = getItem(position);
				try {
					Log.v(APP_NAME,act.optJSONObject("object").toString(3));
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		return view;
	}
	
	private void loadStreams() {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				Pumpio pumpio = new Pumpio(mContext);
				pumpio.setAccount(mAccount);
				JSONObject stream = pumpio.fetchStream(mFeed, null, null, 20);
				JSONArray items = stream.optJSONArray("items");
				mItems = items;

				mHandler.sendLoadComplete();
			}
		};
		new Thread(runnable).start();
	}
	
	
	private void reloadAdapter() {
		if(mItems != null) {
			for(int i=0;i<mItems.length();i++) {
				try {
					JSONObject act = mItems.getJSONObject(i);
					Log.d(APP_NAME,"Act " + i + " item " + act.optString("id")+ " >>>");
					add(act);
				} catch(JSONException e) {
					Log.e(APP_NAME, e.getMessage(),e);
				}
			}
			mItems = null;
		}
		notifyDataSetChanged();
	}
	
	private static class StreamHandler extends Handler {
    	
    	private final WeakReference<ActivityAdapter> mTarget;
    	
    	private static final int MSG_POST_OK = 0;
    	
    	StreamHandler(ActivityAdapter target) {
			mTarget = new WeakReference<ActivityAdapter>(target);
		}
    	
    	public void handleMessage(Message msg) {
    		ActivityAdapter target = mTarget.get();
			
			switch (msg.what) {
			
				
			case MSG_POST_OK:
				target.reloadAdapter();
				break;
			}
		}
    	
    	void sendLoadComplete() {
    		sendEmptyMessage(MSG_POST_OK);
    	}
    }
	
}
