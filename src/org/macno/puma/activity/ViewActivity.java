package org.macno.puma.activity;

import static org.macno.puma.PumaApplication.APP_NAME;
import static org.macno.puma.activity.HomeActivity.ACTION_ACTIVITY_DELETED;

import java.lang.ref.WeakReference;
import java.util.HashMap;

import javax.net.ssl.SSLException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.macno.puma.R;
import org.macno.puma.core.Account;
import org.macno.puma.manager.AccountManager;
import org.macno.puma.provider.Pumpio;
import org.macno.puma.util.ActivityUtil;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ViewActivity extends Activity {

	public static final String EXTRA_ACTIVITY = "extraActivity";
	public static final String EXTRA_FEED = "extraFeed";
	public static final String EXTRA_ACCOUNT_UUID = "extraAccountUUID";
	
	private Account mAccount;
	private JSONObject mActivity;
	private String mFeed;
	
	private Context mContext;
	
	private PostHandler mHandler = new PostHandler(this);
		
	private boolean mLoading = false;
	
	private JSONArray mComments;
	
	private LinearLayout ll_comments;
	
	private Pumpio mPumpio;
	private Animation mRotationAnimation;
	
	private ImageView mLoadingComments;
	
	private boolean mOwnActivity = false;
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setProgressBarIndeterminate(true);
        setProgressBarIndeterminateVisibility(false);
        mContext = this;
        setContentView(R.layout.activity_view);
        Bundle extras = getIntent().getExtras();
        
        String accountUUID = "";
        String activity="";
		if (savedInstanceState != null) {
			accountUUID = savedInstanceState.getString(EXTRA_ACCOUNT_UUID);
			activity = savedInstanceState.getString(EXTRA_ACTIVITY);
			mFeed = savedInstanceState.getString(EXTRA_FEED);
		} else if (extras != null) {
			accountUUID = extras.getString(EXTRA_ACCOUNT_UUID);
			activity = extras.getString(EXTRA_ACTIVITY);
			mFeed = extras.getString(EXTRA_FEED);
		}
		AccountManager am = new AccountManager(this);
		mAccount = am.getAccount(accountUUID);
		
		if(mAccount == null) {
			AccountAddActivity.startActivity(this);
			finish();
		}

		mPumpio = new Pumpio(mContext);
		mPumpio.setAccount(mAccount);
        try {
        	mActivity = new JSONObject(activity);
        } catch(JSONException e) {
        	
        }
        
        
        mRotationAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate);
        
        JSONObject actor = ActivityUtil.getActor(mActivity);
        String acctId = actor.optString("id");
        
        if(acctId != null) {
        	mOwnActivity = acctId.equals(mAccount.getAcctId());
        }
        LinearLayout ll_parent = (LinearLayout)findViewById(R.id.ll_activity_parent);
        ll_parent.addView(ActivityUtil.getViewActivity(mPumpio, mActivity,false, true));

        addLikes();
        
        ll_comments = (LinearLayout)findViewById(R.id.ll_activity_replies);
        addComments();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putString(EXTRA_ACTIVITY, mActivity.toString());
		outState.putString(EXTRA_ACCOUNT_UUID, mAccount.getUuid());
		outState.putString(EXTRA_FEED, mFeed);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	
	private void addComments() {
		JSONObject obj = mActivity.optJSONObject("object");
		if(obj == null) {
			return;
		}
		JSONObject replies = obj.optJSONObject("replies");
		
		if(replies != null) {
			LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			
			JSONArray items = replies.optJSONArray("items");
			if(items != null) {
				int totalItems = replies.optInt("totalItems", 0);
				if(totalItems > items.length()) {
					
					JSONObject pumpIo = replies.optJSONObject("pump_io");
					if(pumpIo != null && pumpIo.has("proxyURL")) {
						// If there's a proxy I use it
						loadComments(pumpIo.optString("proxyURL"));
					} else {
						loadComments(replies.optString("url"));
					}
					addLoadingCommentsText();
				}
				for(int i=items.length()-1;i>=0;i--) {
					LinearLayout view = ActivityUtil.getViewComment(mPumpio, inflater, items.optJSONObject(i), (i % 2 == 0),this);
					if(view != null) {
						ll_comments.addView(view);
					}
				}
			}
		}
	}
	
	private void addLoadingCommentsText() {
		
		mLoadingComments = (ImageView)findViewById(R.id.iv_loading);
		mLoadingComments.startAnimation(mRotationAnimation);
		
		LinearLayout ll_parent = (LinearLayout)findViewById(R.id.ll_comments_loading);
		ll_parent.setVisibility(View.VISIBLE);
		
		
	}
		
	private void loadComments(final String feed) {
		if(mLoading) {
			return;
		}
		//setProgressBarIndeterminateVisibility(true);
		Log.d(APP_NAME,"loading comments from " + feed);
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				mLoading=true;
				try {
					JSONObject stream = mPumpio.fetchStream(feed, null, null, 100);
					if(stream == null) {
						mHandler.sendLoadCommentsFailed();
						return;
					}
					JSONArray items = stream.optJSONArray("items");
					if(items == null) {
						mHandler.sendLoadCommentsFailed();
						return;
					}
					Log.d(APP_NAME,"comments: loaded " + items.length() + " comments");
					if(items.length() == 0){
						// !? ?!
						mHandler.sendLoadCommentsEmpty();
						return;
					}
					
					mComments = items;

					mHandler.sendReloadComments();
				} catch(SSLException e) {
					mHandler.sendLoadCommentsFailed();
				}
			}
		};
		new Thread(runnable).start();
	}
	
	private void addLikes() {
		JSONObject obj = mActivity.optJSONObject("object");
		if(obj == null) {
			return;
		}
		boolean liked = obj.optBoolean("liked",false);
		if(liked) {
			ImageView iv = (ImageView)findViewById(R.id.iv_like);
			iv.setImageResource(R.drawable.favorited);
		}
		JSONObject likes = obj.optJSONObject("likes");
		if(likes != null) {
			int totalItems = likes.optInt("totalItems");
			JSONArray items = likes.optJSONArray("items");
			if(items != null) {
				StringBuilder whoLike = new StringBuilder();
				for(int i=0;i<items.length();i++) {
					JSONObject item = items.optJSONObject(i);
					if(i > 0) {
						whoLike.append(", ");
					}
					String author = item.optString("displayName");
					if(author == null) {
						author =  item.optString("preferredUsername");
					}
					whoLike.append(author);
				}
				TextView tv_likes = (TextView)findViewById(R.id.tv_who_like);
				if(items.length()==1) {
					tv_likes.setText(getString(R.string.who_likes,whoLike.toString()));
				} else if( items.length() > 1 ){
					if(items.length() != totalItems) {
						tv_likes.setText(getString(R.string.who_like_and_more,whoLike.toString(), (totalItems - items.length())));
					} else {
						tv_likes.setText(getString(R.string.who_like,whoLike.toString()));
					}
				}
			}
		}
	}
	public void doFavorite(View v) {
		JSONObject obj = mActivity.optJSONObject("object");
		if(obj == null) {
			return;
		}
		doFavorite(v,obj.optBoolean("liked",false),ActivityUtil.getMinimumObject(obj));
	}
	
	private HashMap<String, View> mFavoriteViewMap = new HashMap<String, View>();
	
	public void doFavorite(View v, final boolean liked, final JSONObject target) {

		v.startAnimation(mRotationAnimation);

		final String id = target.optString("id");
		mFavoriteViewMap.put(id, v);

		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				Pumpio pumpio = new Pumpio(mContext);
				pumpio.setAccount(mAccount);
				
				try {
					if(liked) {
						pumpio.unfavoriteNote(target);
						mHandler.sendUnfavoriteComplete(id);
					} else {
						pumpio.favoriteNote(target);
						mHandler.sendFavoriteComplete(id);
					}

				} catch(Exception e) {
					Log.e(APP_NAME, e.getMessage(),e);
					mHandler.sendFavoriteError(id);
				}
			}
		};
		new Thread(runnable).start();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.view, menu);
		
		if(mOwnActivity) {
			menu.findItem(R.id.action_delete).setVisible(true);
		}
		
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_reply:
			onReplyAction();
			return true;
		case R.id.action_share:
			share();
			return true;
		case R.id.action_delete:
			delete();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void share() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder
		.setMessage(R.string.confirm_share)
		.setCancelable(false)
		.setPositiveButton(android.R.string.yes,
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface xdialog, int id) {
				doShare();
			}
		})
		.setNegativeButton(android.R.string.no,null)
		.create()
		.show();
		
	}
	
	private void doShare() {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				Pumpio pumpio = new Pumpio(mContext);
				pumpio.setAccount(mAccount);
				JSONObject target = ActivityUtil.getMinimumObject(mActivity.optJSONObject("object"));
				pumpio.shareNote(target);
				mHandler.sendShareComplete();
			}
		};
		new Thread(runnable).start();
		finish();
	}
	

	private void delete() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder
		.setMessage(R.string.confirm_delete)
		.setCancelable(false)
		.setPositiveButton(android.R.string.yes,
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface xdialog, int id) {
				doDelete();
			}
		})
		.setNegativeButton(android.R.string.no,null)
		.create()
		.show();
		
	}
	
	private void doDelete() {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				Pumpio pumpio = new Pumpio(mContext);
				pumpio.setAccount(mAccount);
				JSONObject act = mActivity.optJSONObject("object");
				JSONObject target = ActivityUtil.getMinimumObject(act);
				pumpio.deleteNote(target);
				
//				ActivityManager activityManager = new ActivityManager(mContext);
//				activityManager.deleteActivity(mFeed, act);
				
				mHandler.sendDeleteComplete();
			}
		};
		new Thread(runnable).start();
		finish();
	}
	
	
	private void reloadComments() {
//		setProgressBarIndeterminateVisibility(false);
		if(mLoadingComments!=null) {
			mLoadingComments.clearAnimation();
			LinearLayout ll_parent = (LinearLayout)findViewById(R.id.ll_comments_loading);
			ll_parent.setVisibility(View.GONE);
		}
		if(mComments == null) {
			return;
		}
		ll_comments.removeAllViews();
		
		LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		for(int i=mComments.length()-1;i>=0;i--) {
			LinearLayout view = ActivityUtil.getViewComment(mPumpio, inflater, mComments.optJSONObject(i), (i % 2 == 0),this);
			if(view != null) {
				ll_comments.addView(view);
			}
		}
	}
	
	public void doUnfavorComment(View v, JSONObject object) {
		doFavorite(v,true,object);
	}

	public void doFavorComment(View v, JSONObject object) {
		doFavorite(v,false,object);
	}

	private void onFavoritedError(String targetId) {
		
		View iv = mFavoriteViewMap.get(targetId);
		mFavoriteViewMap.remove(targetId);
		iv.clearAnimation();
		Toast.makeText(mContext, getString(R.string.note_favorite_error), Toast.LENGTH_SHORT).show();
	}
	private void onUnfavoritedNote(String targetId) {
		// Switch image..
		ImageView iv = (ImageView)mFavoriteViewMap.get(targetId);
		mFavoriteViewMap.remove(targetId);
		iv.clearAnimation();
		iv.setImageResource(R.drawable.not_favorited);
		Toast.makeText(mContext, getString(R.string.note_unfavorited), Toast.LENGTH_SHORT).show();
	}
	
	private void onFavoritedNote(boolean notify,String targetId) {
		// Switch image..
		
		ImageView iv = (ImageView)mFavoriteViewMap.get(targetId);
		mFavoriteViewMap.remove(targetId);
		iv.clearAnimation();
		iv.setImageResource(R.drawable.favorited);
		if(notify) {
			Toast.makeText(mContext, getString(R.string.note_favorited), Toast.LENGTH_SHORT).show();
		}
	}
	
	private void onSharedNote() {
		Toast.makeText(mContext, getString(R.string.note_shared), Toast.LENGTH_SHORT).show();
	}
	
	private void onDeletedNote() {
		broadcastIntentReload();
	}
	
	private void notifyLoadCommentsFailed() {
		//setProgressBarIndeterminateVisibility(false);
		if(mLoadingComments != null) {
			mLoadingComments.clearAnimation();
			LinearLayout ll_parent = (LinearLayout)findViewById(R.id.ll_comments_loading);
			ll_parent.setVisibility(View.GONE);
		}
		Toast.makeText(mContext, R.string.load_comments_failed, Toast.LENGTH_LONG).show();
	}
	
	private void notifyLoadCommentsEmpty() {
//		setProgressBarIndeterminateVisibility(false);
		if(mLoadingComments!=null) {
			mLoadingComments.clearAnimation();
			LinearLayout ll_parent = (LinearLayout)findViewById(R.id.ll_comments_loading);
			ll_parent.setVisibility(View.GONE);
		}
	}
	
	private static class PostHandler extends Handler {
    	
		private final WeakReference<ViewActivity> mTarget;
    	
    	private static final int MSG_SHARE_OK = 0;
    	private static final int MSG_RELOADED_NEWER = 1;
    	private static final int MSG_LOAD_COMMENTS_FAILED = 2;
    	private static final int MSG_FAV_OK = 4;
    	private static final int MSG_UNFAV_OK = 5;
    	private static final int MSG_FAV_ERROR = 6;
    	private static final int MSG_LOAD_COMMENTS_EMPTY = 7;
    	private static final int MSG_DELETE_OK = 8;
    	
    	PostHandler(ViewActivity target) {
			mTarget = new WeakReference<ViewActivity>(target);
		}
    	
    	public void handleMessage(Message msg) {
    		ViewActivity target = mTarget.get();
    		Bundle data = msg.getData();
			switch (msg.what) {
			
			case MSG_SHARE_OK:
				target.onSharedNote();
				break;
				
			case MSG_FAV_OK:
				target.onFavoritedNote(true,data.getString("targetId"));
				break;
			
			case MSG_UNFAV_OK:
				target.onUnfavoritedNote(data.getString("targetId"));
				break;
				
			case MSG_FAV_ERROR:
				target.onFavoritedError(data.getString("targetId"));
				break;
				
			case MSG_RELOADED_NEWER:
				target.reloadComments();
				break;
			case MSG_LOAD_COMMENTS_EMPTY:
				target.notifyLoadCommentsEmpty();
				break;
				
			case MSG_LOAD_COMMENTS_FAILED:
				target.notifyLoadCommentsFailed();
				break;
			case MSG_DELETE_OK:
				target.onDeletedNote();
				break;
	    	
			}
			

		}
    	
    	void sendDeleteComplete() {
    		sendEmptyMessage(MSG_DELETE_OK);
    	}
    	void sendShareComplete() {
    		sendEmptyMessage(MSG_SHARE_OK);
    	}
    	
    	void sendReloadComments() {
    		sendEmptyMessage(MSG_RELOADED_NEWER);
    	}
    	
    	void sendLoadCommentsFailed() {
    		sendEmptyMessage(MSG_LOAD_COMMENTS_FAILED);
    	}
    	
    	void sendLoadCommentsEmpty() {
    		sendEmptyMessage(MSG_LOAD_COMMENTS_EMPTY);
    	}
    	
    	void sendFavoriteComplete(String targetId) {
    		Message msg = new Message();
    		msg.what = MSG_FAV_OK;
    		Bundle data = new Bundle();
    		data.putString("targetId", targetId);
    		msg.setData(data);
    		sendMessage(msg);
    	}
    	void sendUnfavoriteComplete(String targetId) {
    		Message msg = new Message();
    		msg.what = MSG_UNFAV_OK;
    		Bundle data = new Bundle();
    		data.putString("targetId", targetId);
    		msg.setData(data);
    		sendMessage(msg);
    	}
    	
    	void sendFavoriteError(String targetId) {
    		Message msg = new Message();
    		msg.what = MSG_FAV_ERROR;
    		Bundle data = new Bundle();
    		data.putString("targetId", targetId);
    		msg.setData(data);
    		sendMessage(msg);
    	}
    }
	
	private void onReplyAction() {
		ComposeActivity.startActivity(mContext,mAccount,mActivity.optJSONObject("object").toString(), ComposeActivity.ACTION_REPLY );
	}
	
	public static void startActivity(Context context,Account account, String feed, JSONObject activity) {
		Intent viewActivityIntent = new Intent(context,ViewActivity.class);
		viewActivityIntent.putExtra(ViewActivity.EXTRA_ACTIVITY, activity.toString());
		viewActivityIntent.putExtra(ViewActivity.EXTRA_FEED, feed);
		viewActivityIntent.putExtra(ViewActivity.EXTRA_ACCOUNT_UUID, account.getUuid());
		context.startActivity(viewActivityIntent);
	}
	
	private void broadcastIntentReload() {
        Intent intent = new Intent(ACTION_ACTIVITY_DELETED);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

}
