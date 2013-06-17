package org.macno.puma.adapter;

import static org.macno.puma.PumaApplication.APP_NAME;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.macno.puma.activity.ViewActivity;
import org.macno.puma.core.Account;
import org.macno.puma.provider.Pumpio;
import org.macno.puma.util.ActivityUtil;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;

public class ActivityAdapter extends ArrayAdapter<JSONObject> {

	private String mFeed;
	private Context mContext;
	private Account mAccount;
	private StreamHandler mHandler = new StreamHandler(this);

	private JSONArray mItems;
	
	private Pumpio mPumpio;

	private boolean mLoading = false;
	
	public ActivityAdapter(Context context, Account account, String feed) {
		super(context,0,new ArrayList<JSONObject>());
		mContext = context;
		mAccount = account;
		mFeed = feed;
		loadStreams();
	}
	
	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		
		JSONObject act  = getItem(position);
		LinearLayout view = ActivityUtil.getViewActivity(getContext(), act);
		view.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				JSONObject act = getItem(position);
				openViewActivity(act);
			}
		});

		return view;
	}
	
	private void openViewActivity(JSONObject act) {
		ViewActivity.startActivity(mContext, mAccount, act);
	}
	
	public void checkNewActivities() {
		loadStreamsForNewer();
	}
	
	private void loadStreams() {
		if(mLoading) {
			return;
		}
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				mLoading=true;
				if(mPumpio == null) {
					mPumpio = new Pumpio(mContext);
					mPumpio.setAccount(mAccount);
				}
				JSONObject stream = mPumpio.fetchStream(mFeed, null, null, 20);
				JSONArray items = stream.optJSONArray("items");
				mItems = items;

				mHandler.sendLoadComplete();
			}
		};
		new Thread(runnable).start();
	}
	
	private void loadStreamsForNewer() {
		if(mLoading) {
			return;
		}
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				mLoading=true;
				if(mPumpio == null) {
					mPumpio = new Pumpio(mContext);
					mPumpio.setAccount(mAccount);
				}
				JSONObject last = getItem(0);
				JSONObject stream = mPumpio.fetchStream(mFeed, last.optJSONObject("object").optString("id"), null, 20);
				JSONArray items = stream.optJSONArray("items");
				mItems = items;

				mHandler.sendReloadedNewer();
			}
		};
		new Thread(runnable).start();
	}
	
	private void reloadAdapter(boolean newer) {
		mLoading=false;
		if(mItems != null) {
			for(int i=0;i<mItems.length();i++) {
				try {
					JSONObject act = mItems.getJSONObject(i);
					if(act.has("object")) {
						if(newer) {
							insert(act, 0);
						} else {
							add(act);
						}
					}
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
    	private static final int MSG_RELOADED_NEWER = 1;
    	
    	StreamHandler(ActivityAdapter target) {
			mTarget = new WeakReference<ActivityAdapter>(target);
		}
    	
    	public void handleMessage(Message msg) {
    		ActivityAdapter target = mTarget.get();
			
			switch (msg.what) {
			
			case MSG_RELOADED_NEWER:
				target.reloadAdapter(true);
				break;
				
			case MSG_POST_OK:
				target.reloadAdapter(false);
				break;
			}
		}
    	
    	void sendLoadComplete() {
    		sendEmptyMessage(MSG_POST_OK);
    	}
    	
    	void sendReloadedNewer() {
    		sendEmptyMessage(MSG_RELOADED_NEWER);
    	}
    }
	
}
