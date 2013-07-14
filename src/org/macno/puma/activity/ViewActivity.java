package org.macno.puma.activity;

import static org.macno.puma.PumaApplication.APP_NAME;

import java.lang.ref.WeakReference;

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
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ViewActivity extends Activity {

	public static final String EXTRA_ACTIVITY = "extraActivity";
	public static final String EXTRA_ACCOUNT_UUID = "extraAccountUUID";
	
	private Account mAccount;
	private JSONObject mActivity;
	private Context mContext;
	
	private PostHandler mHandler = new PostHandler(this);
	
	private boolean mLoading = false;
	
	private JSONArray mComments;
	
	private LinearLayout ll_comments;
	
	private Pumpio mPumpio;
	
	private TextView mLoadingComments;
	
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
		} else if (extras != null) {
			accountUUID = extras.getString(EXTRA_ACCOUNT_UUID);
			activity = extras.getString(EXTRA_ACTIVITY);
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
        
        LinearLayout ll_parent = (LinearLayout)findViewById(R.id.ll_activity_parent);
        ll_parent.addView(ActivityUtil.getViewActivity(mContext, mActivity,false, true));

        addLikes(ll_parent);
        
        ll_comments = (LinearLayout)findViewById(R.id.ll_activity_replies);
        addComments(ll_comments);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putString(EXTRA_ACTIVITY, mActivity.toString());
		outState.putString(EXTRA_ACCOUNT_UUID, mAccount.getUuid());
		super.onSaveInstanceState(outState);
	}

	private void addComments(LinearLayout ll_parent ) {
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
					addLoadingCommentsText(ll_parent);
				}
				for(int i=items.length()-1;i>=0;i--) {
					LinearLayout view = ActivityUtil.getViewComment(mContext, inflater, items.optJSONObject(i), (i % 2 == 0));
					if(view != null) {
						ll_parent.addView(view);
					}
				}
			}
		}
	}
	
	private void addLoadingCommentsText(LinearLayout ll_parent) {
		mLoadingComments = new TextView(mContext);
		mLoadingComments.setText(R.string.loading_comments_1);
		ll_parent.addView(mLoadingComments,LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				int[] loading = {
						R.string.loading_comments_1, 
						R.string.loading_comments_2, 
						R.string.loading_comments_3,
						R.string.loading_comments_4
					};
				int c=0;
				while(mLoadingComments != null) {
					mHandler.changeLoadingComment(loading[c]);
					c++;
					if(c >= loading.length) {
						c=0;
					}
					try {
						Thread.sleep(400);
					} catch (InterruptedException e) {
						Log.e(APP_NAME,e.getMessage(),e);
						return;
					}
				}
			}
		};
		new Thread(runnable).start();
	}
		
	private void loadComments(final String feed) {
		if(mLoading) {
			return;
		}
		setProgressBarIndeterminateVisibility(true);
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
					mComments = items;

					mHandler.sendReloadComments();
				} catch(SSLException e) {
					mHandler.sendLoadCommentsFailed();
				}
			}
		};
		new Thread(runnable).start();
	}
	
	private void addLikes(LinearLayout ll_parent ) {
		JSONObject obj = mActivity.optJSONObject("object");
		if(obj == null) {
			return;
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
				TextView tv_likes = new TextView(mContext);
				tv_likes.setPadding(5, 5, 5, 5);
				tv_likes.setTextSize(12);
				if(items.length()==1) {
					tv_likes.setText(getString(R.string.who_likes,whoLike.toString()));
				} else if( items.length() > 1 ){
					if(items.length() != totalItems) {
						tv_likes.setText(getString(R.string.who_like_and_more,whoLike.toString(), (totalItems - items.length())));
					} else {
						tv_likes.setText(getString(R.string.who_like,whoLike.toString()));
					}
				}
				ll_parent.addView(tv_likes);
			}
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.view, menu);
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
			
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void share() {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				Pumpio pumpio = new Pumpio(mContext);
				pumpio.setAccount(mAccount);
				pumpio.shareNote(mActivity.optJSONObject("object"));
				mHandler.sendShareComplete();
			}
		};
		new Thread(runnable).start();
		finish();
	}
	
	private void reloadComments() {
		setProgressBarIndeterminateVisibility(false);
		
		if(mComments == null) {
			return;
		}
		ll_comments.removeAllViews();
		if(mLoadingComments != null) {
			mLoadingComments = null;
		}
		LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		for(int i=mComments.length()-1;i>=0;i--) {
			LinearLayout view = ActivityUtil.getViewComment(mContext, inflater, mComments.optJSONObject(i), (i % 2 == 0));
			if(view != null) {
				ll_comments.addView(view);
			}
		}
	}
	
	private void onSharedNote() {
		Toast.makeText(mContext, getString(R.string.note_shared), Toast.LENGTH_SHORT).show();
	}
	
	private void notifyLoadCommentsFailed() {
		setProgressBarIndeterminateVisibility(false);
		if(mLoadingComments != null) {
			mLoadingComments = null;
		}
		Toast.makeText(mContext, R.string.load_comments_failed, Toast.LENGTH_LONG).show();
	}
	
	private void changeLoadingComment(int string) {
		if(mLoadingComments != null) {
			mLoadingComments.setText(string);
		}
	}
	
	private static class PostHandler extends Handler {
    	
		private final WeakReference<ViewActivity> mTarget;
    	
    	private static final int MSG_SHARE_OK = 0;
    	private static final int MSG_RELOADED_NEWER = 1;
    	private static final int MSG_LOAD_COMMENTS_FAILED = 2;
    	private static final int MSG_CHANGE_COMMENT = 3;
    	
    	PostHandler(ViewActivity target) {
			mTarget = new WeakReference<ViewActivity>(target);
		}
    	
    	public void handleMessage(Message msg) {
    		ViewActivity target = mTarget.get();
    		
			switch (msg.what) {
			
			case MSG_SHARE_OK:
				target.onSharedNote();
				break;
				
			case MSG_RELOADED_NEWER:
				target.reloadComments();
				break;
				
			case MSG_LOAD_COMMENTS_FAILED:
				target.notifyLoadCommentsFailed();
				break;
				
	    	case MSG_CHANGE_COMMENT:
	    		target.changeLoadingComment(msg.arg1);
	    		break;
			}
			

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
    	    	
    	void changeLoadingComment(int string) {
    		Message msg = new Message();
			msg.what=MSG_CHANGE_COMMENT;
			msg.arg1=string;
			sendMessage(msg);
    	}
    }
	
	private void onReplyAction() {
		ComposeActivity.startActivity(mContext,mAccount,mActivity.optJSONObject("object").toString(), ComposeActivity.ACTION_REPLY );
	}
	
	public static void startActivity(Context context,Account account, JSONObject activity) {
		Intent viewActivityIntent = new Intent(context,ViewActivity.class);
		viewActivityIntent.putExtra(ViewActivity.EXTRA_ACTIVITY, activity.toString());
		viewActivityIntent.putExtra(ViewActivity.EXTRA_ACCOUNT_UUID, account.getUuid());
		context.startActivity(viewActivityIntent);
	}

}
