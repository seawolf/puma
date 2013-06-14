package org.macno.puma.activity;

import static org.macno.puma.PumaApplication.APP_NAME;

import java.lang.ref.WeakReference;
import java.util.Locale;

import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import oauth.signpost.exception.OAuthException;

import org.json.JSONException;
import org.json.JSONObject;
import org.macno.puma.R;
import org.macno.puma.manager.OAuthManager;
import org.macno.puma.util.HttpUtil;
import org.macno.puma.util.HttpUtilException;

import android.app.Activity;
import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class AccountAddActivity extends Activity {
	
	private static final String REQUEST_TOKEN_URL = "https://%s/oauth/request_token";
	private static final String AUTHORIZE_URL     = "https://%s/oauth/authorize";
	private static final String ACCESS_TOKEN_URL = "https://%s/oauth/access_token";
	
	private static final String WHOAMI_URL = "%s/api/whoami";
	
	private static final String EMAIL_PATTERN = 
			"^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
			+ "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
	
	private EditText mWebfingerID;
	private Button mNext;
	private WebView mOAuthView;
	private ViewGroup mWebfingerView;
	private ViewGroup mProgressView;
	
	private String mUsername;
	private String mHost;
	
	private CommonsHttpOAuthConsumer	mConsumer;
	private CommonsHttpOAuthProvider 	mProvider;
	
	private HttpUtil mHttpUtil;
	private LoginHandler mHandler = new LoginHandler(this);
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_add);
        
        mNext = (Button)findViewById(R.id.btn_next);
		mNext.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				startDancing();
			}
		});
		
		mWebfingerID = (EditText)findViewById(R.id.et_webfinger_id);
		mWebfingerID.addTextChangedListener(mTextWatcher);
		
		mOAuthView = (WebView)findViewById(R.id.wv_oauth);
		mOAuthView.setWebViewClient(new OAuthWebListener());
		
		mWebfingerView = (ViewGroup)findViewById(R.id.rl_webfinger);
		mProgressView = (ViewGroup)findViewById(R.id.rl_auth_progress);
		
	}

	private void switchView(View view) {
		mWebfingerView.setVisibility(view == mWebfingerView ? View.VISIBLE : View.GONE);
		mOAuthView.setVisibility(view == mOAuthView ? View.VISIBLE : View.GONE);
		mProgressView.setVisibility(view == mProgressView ? View.VISIBLE : View.GONE);
	}

	private void startDancing() {
		
		mNext.setEnabled(false);
		
		// Parse webfinger..
		String webfinger = mWebfingerID.getText().toString();
		int atpos = webfinger.indexOf("@");
		if(atpos <= 0) {
			// WTF!?
			mNext.setEnabled(true);
			return;
		}
		mUsername = webfinger.substring(0,atpos);
		mHost = webfinger.substring(atpos+1);
		
		mHttpUtil = new HttpUtil(mHost);
		
		getRequestTokenURL();
		
	}
	
	private TextWatcher mTextWatcher = new TextWatcher() {

		@Override
		public void afterTextChanged(Editable e) {
			if(e.length() == 0 && mNext.isEnabled()) {
				mNext.setEnabled(false);
				mWebfingerID.setError(null);
			} else if(e.length() > 0 ) {
				if (e.toString().toLowerCase(Locale.ROOT).matches(EMAIL_PATTERN) ) {
			    	mWebfingerID.setError(null);
			    	if(!mNext.isEnabled()) {
			    		mNext.setEnabled(true);
			    	}
	            } else {
	            	mWebfingerID.setError(getString(R.string.webfinger_not_valid));
	            	if(mNext.isEnabled()) {
	            		mNext.setEnabled(false);
	            	}
	            }
	        	
			}
			
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
		}

	};
	
	private void getRequestTokenURL() {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				
				try {
					OAuthManager oauthManager = new OAuthManager(AccountAddActivity.this);
					
//					// ONLY FOR TESTING PURPOSE!!!! 
//					// REMOVE BEFORE PRODUCTION!!
//					oauthManager.removeConsumerForHost(mHost);
					
					mConsumer = oauthManager.getConsumerForHost(mHost);
					
					Log.d(APP_NAME,""+mConsumer.getConsumerKey());
					Log.d(APP_NAME,""+mConsumer.getConsumerSecret());
					Log.d(APP_NAME,""+mConsumer.getToken());
					Log.d(APP_NAME,""+mConsumer.getTokenSecret());
					
				} catch(Exception e) {
					Log.e(APP_NAME, "Error getting consumer", e);
					mHandler.errorOAuthURL(e.getMessage());
					return;
				}

				mProvider = new CommonsHttpOAuthProvider(
						String.format(REQUEST_TOKEN_URL, mHost), 
						String.format(ACCESS_TOKEN_URL, mHost),
						String.format(AUTHORIZE_URL, mHost),mHttpUtil.getHttpClient());

				try {
					String url = mProvider.retrieveRequestToken(mConsumer, "https://puma.macno.org/fake/oauth_callback");
					
					Log.d(APP_NAME,""+mConsumer.getConsumerKey());
					Log.d(APP_NAME,""+mConsumer.getConsumerSecret());
					Log.d(APP_NAME,""+mConsumer.getToken());
					Log.d(APP_NAME,""+mConsumer.getTokenSecret());
					
					mHandler.openOAuthWebPage(url);
				} catch(OAuthException e) {
					Log.e(APP_NAME, "Erorr getting request token", e);
					mHandler.errorOAuthURL(e.getMessage());
				}

			}
		};
		new Thread(runnable).start();
	}
	
	private void notifyOAuthError(String message) {
		mNext.setEnabled(true);
		switchView(mWebfingerView);
		Toast.makeText(this, message, Toast.LENGTH_LONG).show();
	}
	
	
	private void openOAuthWebPage(String url) {
		mOAuthView.loadUrl(url);
		switchView(mOAuthView);
	}
	
	private void parseOAuthCallback(final Uri uri) {
		
		Log.v(APP_NAME, uri.getQuery());
		String oauth_problem = uri.getQueryParameter("oauth_problem");
		if (oauth_problem != null && !"".equals(oauth_problem)) {
			String message = "";
		
			if(oauth_problem.equals("user_refused")) {
				message = getString(R.string.oauth_error_user_refused);
			} else {
				message= getString(R.string.error_generic_detail,oauth_problem);
			}
			switchView(mWebfingerView);
			new AlertDialog.Builder(this)
				.setTitle(getString(R.string.error))
				.setMessage(message)
				.setNeutralButton(getString(android.R.string.ok), null).show();
			return;
		}
		switchView(mProgressView);
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				
				try {
					String oauthVerifier = uri.getQueryParameter("oauth_verifier");
					
					mProvider.retrieveAccessToken(mConsumer,oauthVerifier);
					
					Log.d(APP_NAME,""+mConsumer.getConsumerKey());
					Log.d(APP_NAME,""+mConsumer.getConsumerSecret());
					Log.d(APP_NAME,""+mConsumer.getToken());
					Log.d(APP_NAME,""+mConsumer.getTokenSecret());
					
				} catch(OAuthException e) {
					Log.e(APP_NAME, "Error getting access token", e);
					return;
				}

				
				
				String baseURL = String.format(WHOAMI_URL, mHost);
				JSONObject juser = null;
				try {
					juser = mHttpUtil.getJsonObject("https://"+baseURL);
				} catch(HttpUtilException e) {
					e.printStackTrace();
					//non https
					try {
						juser = mHttpUtil.getJsonObject("http://"+baseURL);
					} catch (HttpUtilException e2) {
						e2.printStackTrace();
						mHandler.errorOAuthURL(e2.getMessage());
						return;
					}
				}
				try {
					Log.d(APP_NAME,juser.toString(3));
					String preferredUsername = juser.getString("preferredUsername");
					if(preferredUsername.equals(mUsername)) {
						// Ok
					}
				} catch (JSONException e) {
					Log.e(APP_NAME,e.getMessage(),e);
				}
				
			}
		};
		new Thread(runnable).start();
	}
	
	private static class LoginHandler extends Handler {

		private static final String K_TOKEN_URL = "tokenURL";
		private static final String K_ERROR_MESSAGE = "errorMessage";
		
		private final WeakReference<AccountAddActivity> mTarget; 

		static final int MSG_OPEN_OAUTH_PAGE = 0;
		static final int MSG_ERROR_OAUTH = 1;

		LoginHandler(AccountAddActivity target) {
			mTarget = new WeakReference<AccountAddActivity>(target);
		}

		public void handleMessage(Message msg) {
			AccountAddActivity target = mTarget.get();
			Bundle data = msg.getData();
			
			switch (msg.what) {
			case MSG_OPEN_OAUTH_PAGE:
				target.openOAuthWebPage(data.getString(K_TOKEN_URL));
				break;
				
			case MSG_ERROR_OAUTH:
				target.notifyOAuthError(data.getString(K_ERROR_MESSAGE));
				break;
			}
		}
		
		void openOAuthWebPage(String url) {
			Message msg = new Message();
			msg.what=MSG_OPEN_OAUTH_PAGE;
			Bundle data = new Bundle();
			data.putString(K_TOKEN_URL, url);
			msg.setData(data);
			sendMessage(msg);
		}
		
		void errorOAuthURL(String error) {
			Message msg = new Message();
			msg.what=MSG_ERROR_OAUTH;
			Bundle data = new Bundle();
			data.putString(K_ERROR_MESSAGE, error);
			msg.setData(data);
			sendMessage(msg);
		}
	}
	
	private class OAuthWebListener extends WebViewClient {
		
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			
			Uri uri = Uri.parse(url);
			
			if(uri.getHost().equals("puma.macno.org")) {
				// Close OAUTH WebView
				Log.d(APP_NAME,"Intercepted puma.macno.org URL loading");
				parseOAuthCallback(uri);
				return true;
			} else return false;
		
		 }	
	}
	

}
