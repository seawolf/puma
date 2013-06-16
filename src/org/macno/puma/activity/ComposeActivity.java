package org.macno.puma.activity;

import static org.macno.puma.PumaApplication.APP_NAME;
import static org.macno.puma.PumaApplication.K_PUMA_SETTINGS;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import org.macno.puma.R;
import org.macno.puma.core.Account;
import org.macno.puma.manager.AccountManager;
import org.macno.puma.provider.Pumpio;
import org.macno.puma.util.LocationUtil;
import org.markdown4j.Markdown4jProcessor;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class ComposeActivity extends Activity {

	public static final String EXTRA_ACCOUNT_UUID = "extraAccountUUID";

	private static final String K_GEO_ENABLED = "geoEnabled";
	private static final String K_PUBLIC_NOTE = "publicNote";
	
	private static final String K_GEO_ENABLED_GLOBALLY = "geoEnabledGlobally";

	private Account mAccount;
	private ArrayList<Account> mAccounts;
	
	private NotificationManager mNotificationManager;
	private SharedPreferences mSettings;

	private Context mContext;

	private EditText mTitle;
	private EditText mNote;
	private CheckBox mCheckBoxLocation;
	private CheckBox mCheckBoxPublic;

	private PostHandler mHandler = new PostHandler(this);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		setContentView(R.layout.activity_compose);
		Bundle extras = getIntent().getExtras();

		String accountUUID = "";
		if (savedInstanceState != null) {
			accountUUID = savedInstanceState.getString(EXTRA_ACCOUNT_UUID);
		} else if (extras != null) {
			accountUUID = extras.getString(EXTRA_ACCOUNT_UUID);
		}

		AccountManager am = new AccountManager(this);
		if("".equals(accountUUID)) {
			mAccount = am.getDefaultAccount();
		} else {
			mAccount = am.getAccount(accountUUID);
		}

		if(mAccount == null) {
			AccountAddActivity.startActivity(this);
			finish();
		}

		boolean multiAccount = true;
		
		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mSettings = getSharedPreferences(K_PUMA_SETTINGS,Context.MODE_PRIVATE);
		
		
		mTitle = (EditText)findViewById(R.id.title);
		mNote = (EditText)findViewById(R.id.note);
		
		
		mCheckBoxLocation = (CheckBox)findViewById(R.id.enable_location);
		handleGeoCheckbox();
		
		mCheckBoxPublic = (CheckBox)findViewById(R.id.public_post);
		handlePublicCheckbox();
		
		mAccounts = am.getAccounts();
		if(mAccounts.size() == 1) {
			multiAccount= false;
		}
		
		if(multiAccount) {
			setAccountsSpinner();
		} else {
			hideSpinner();
		}
		
		mNotificationManager.cancel(0);
		
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.compose, menu);
        return true;
    }
    
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case R.id.action_post:
	        	onPostAction();
	        	return true;
	        
	        	
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
    
    private void postComplete() {
    	Toast.makeText(this, R.string.post_complete, Toast.LENGTH_SHORT).show();
    }
    
    private static class PostHandler extends Handler {
    	
    	private final WeakReference<ComposeActivity> mTarget;
    	
    	private static final int MSG_POST_OK = 0;
    	
    	PostHandler(ComposeActivity target) {
			mTarget = new WeakReference<ComposeActivity>(target);
		}
    	
    	public void handleMessage(Message msg) {
    		ComposeActivity target = mTarget.get();
			
			switch (msg.what) {
			
				
			case MSG_POST_OK:
				target.postComplete();
				break;
			}
		}
    	
    	void sendPostComplete() {
    		sendEmptyMessage(MSG_POST_OK);
    	}
    }
    
	private void hideSpinner() {
		findViewById(R.id.ll_spinner).setVisibility(View.GONE);
	}

	private void setAccountsSpinner() {
		Spinner spinner = (Spinner) findViewById(R.id.account_spinner);
		
		
		ArrayList<CharSequence> accounts = new ArrayList<CharSequence>();
		accounts.add(mAccount.getUsername()+"@"+mAccount.getNode());
		for (Account account : mAccounts) {
			if(account.getUuid().equals(mAccount.getUuid())) {
				continue;
			}
			accounts.add(account.getUsername()+"@"+account.getNode());
		}
		ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, accounts);
		
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		// Apply the adapter to the spinner
		spinner.setAdapter(adapter);
	}

	private void handleGeoCheckbox( ) {
		
		mCheckBoxLocation = (CheckBox)findViewById(R.id.enable_location);
		boolean geoEnabled = mSettings.getBoolean(K_GEO_ENABLED_GLOBALLY, true);
		if(geoEnabled) {

			if(mSettings.getBoolean(K_GEO_ENABLED, false))
				mCheckBoxLocation.setChecked(true);
			mCheckBoxLocation.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

				public void onCheckedChanged(CompoundButton buttonView,
						boolean isChecked) {
					mSettings.edit().putBoolean(K_GEO_ENABLED, isChecked).commit();
				}

			});
		} else {
			mCheckBoxLocation.setChecked(false);
			mCheckBoxLocation.setVisibility(View.GONE);
		}
	}

	private void handlePublicCheckbox( ) {
		
		
			if(mSettings.getBoolean(K_PUBLIC_NOTE, false))
				mCheckBoxPublic.setChecked(true);
			mCheckBoxPublic.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

				public void onCheckedChanged(CompoundButton buttonView,
						boolean isChecked) {
					mSettings.edit().putBoolean(K_PUBLIC_NOTE, isChecked).commit();
				}

			});
		
	}

	private void onPostAction() {
		Spinner spinner = (Spinner) findViewById(R.id.account_spinner);
		Account current = mAccount;
		if(spinner.isShown()) {
			Object o = spinner.getSelectedItem();
			Log.d(APP_NAME,"Spinner obj: " + o);
			current = findAccountByName((String)o);
		}
		
		String title = mTitle.getText().toString();
		String note = mNote.getText().toString();
		try {
			note = new Markdown4jProcessor().process(note);
		} catch(IOException e) {
			Log.e(APP_NAME,e.getMessage(),e);
		}
		Location location = null;
		if(mCheckBoxLocation.isChecked()) {
			LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
			location = LocationUtil.getMostRecentLastKnownLocation(locationManager);
		}
		post(current,title,note, mCheckBoxPublic.isChecked(),location);
		
	}
	
	private void post(final Account account, final String title, final String note, final boolean publicNote, final Location location ) {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				Pumpio pumpio = new Pumpio(mContext);
				pumpio.setAccount(account);
				pumpio.postNote(null, title, note, null, publicNote, location);
				mHandler.sendPostComplete();
			}
		};
		new Thread(runnable).start();
		finish();
	}
	
	private Account findAccountByName(String name) {
		for (Account account : mAccounts) {
			if(name.equals(account.getUsername()+"@"+account.getNode())) {
				return account;
			}
		}
		return null;
	}
	
	public static void startActivity(Context context,Account account) {
		Intent homeIntent = new Intent(context,ComposeActivity.class);
		homeIntent.putExtra(HomeActivity.EXTRA_ACCOUNT_UUID, account.getUuid());
		context.startActivity(homeIntent);
	}

	public static void startActivity(Context context) {
		Intent homeIntent = new Intent(context,ComposeActivity.class);
		context.startActivity(homeIntent);
	}

}
