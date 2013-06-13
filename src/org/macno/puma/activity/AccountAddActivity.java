package org.macno.puma.activity;

import static org.macno.puma.PumaApplication.APP_NAME;

import java.util.Locale;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.exception.OAuthException;

import org.macno.puma.R;
import org.macno.puma.manager.OAuthManager;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class AccountAddActivity extends Activity {
	
	private static final String REQUEST_TOKEN_URL = "https://%s/oauth/request_token";
	private static final String AUTHORIZE_URL     = "https://%s/oauth/authorize";
	private static final String ACCESS_TOKEN_URL = "https://%s/oauth/access_token";
	
	private TextView m_idView;
	private EditText mWebfingerID;
	private Button mNext;
	private WebView mOAuthView;
	private ViewGroup mWebfingerView;
	
	private String mUsername;
	private String mHost;
	
	private OAuthConsumer		mConsumer;
	private OAuthProvider 		mProvider;
	
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
		mWebfingerView = (ViewGroup)findViewById(R.id.rl_webfinger);
	}

	private void showView(View which) {
		mWebfingerView.setVisibility(which == mWebfingerView ? View.VISIBLE : View.GONE);
		mOAuthView.setVisibility(which == mOAuthView ? View.VISIBLE : View.GONE);
	}

	private void startDancing() {
		
		// Parse webfinger..
		
		RequestTokenTask rtt = new RequestTokenTask();
		rtt.execute();
		
	}
	
	private TextWatcher mTextWatcher = new TextWatcher() {

		@Override
		public void afterTextChanged(Editable e) {
			if(e.length() == 0 && mNext.isEnabled()) {
				mNext.setEnabled(false);
				mWebfingerID.setError(null);
			} else if(e.length() > 0 ) {
				if (e.toString().toLowerCase(Locale.ROOT).matches("[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
			            "\\@" +
			            "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
			            "(" +
			                "\\." +
			                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
			            ")+") ) {
			    	mWebfingerID.setError(null);
			    	if(!mNext.isEnabled()) {
			    		mNext.setEnabled(true);
			    	}
	            } else {
	            	if(mWebfingerID.getError() == null)
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
	
	
	private class RequestTokenTask extends AsyncTask<Void, Void, String> {

		@Override
		protected String doInBackground(Void... params) {
			// TODO Auto-generated method stub		
			try {
				OAuthManager oauthManager = new OAuthManager(AccountAddActivity.this);
				mConsumer = oauthManager.getConsumerForHost(mHost);
			} catch(Exception e) {
				Log.e(APP_NAME, "Error getting consumer", e);
				return null;
			}

			mProvider = new DefaultOAuthProvider(
					String.format(REQUEST_TOKEN_URL, mHost), 
					String.format(ACCESS_TOKEN_URL, mHost),
					String.format(AUTHORIZE_URL, mHost));

			try {
				return mProvider.retrieveRequestToken(mConsumer, "https://impeller.e43.eu/DUMMY_OAUTH_CALLBACK");
			} catch(OAuthException e) {
				Log.e(APP_NAME, "Erorr getting request token", e);
				return null;
			}
		}

		protected void onPostExecute(final String tokenUrl) {
			if(tokenUrl != null) {
				mOAuthView.loadUrl(tokenUrl);
				showView(mOAuthView);
			} else {
				m_idView.setError("Error communicating with server");
				showView(mWebfingerView);
			}
		}		
	}
}
