package org.macno.puma.activity;

import static org.macno.puma.PumaApplication.APP_NAME;
import static org.macno.puma.PumaApplication.K_PUMA_SETTINGS;
import static org.macno.puma.activity.HomeActivity.ACTION_ACTIVITY_POSTED;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;
import org.macno.puma.R;
import org.macno.puma.core.Account;
import org.macno.puma.manager.AccountManager;
import org.macno.puma.provider.Pumpio;
import org.macno.puma.util.ImageUtil;
import org.macno.puma.util.LocationUtil;
import org.markdown4j.Markdown4jProcessor;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.ImageColumns;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class ComposeActivity extends Activity {

	public static final int ACTION_REPLY = 1;

	public static final String EXTRA_ACCOUNT_UUID = "extraAccountUUID";
	public static final String EXTRA_ACTIVITY = "extraActivity";
	public static final String EXTRA_ACTION = "extraAction";

	private static final String K_GEO_ENABLED = "geoEnabled";
	private static final String K_PUBLIC_NOTE = "publicNote";

	private static final String K_GEO_ENABLED_GLOBALLY = "geoEnabledGlobally";

	private final int CHOOSE_FILE_ID=0;

	private Account mAccount;
	private ArrayList<Account> mAccounts;

	private NotificationManager mNotificationManager;
	private SharedPreferences mSettings;

	private Context mContext;

	private EditText mTitle;
	private EditText mNote;
	private TextView mTextViewFileName;
	private CheckBox mCheckBoxLocation;
	private CheckBox mCheckBoxPublic;

	private PostHandler mHandler = new PostHandler(this);

	private boolean mPreserveAccount=false;
	private String mActivityId;
	private int mAction = 0;

	private File mFilename;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		setContentView(R.layout.activity_compose);
		Bundle extras = getIntent().getExtras();

		// Get details of Share content by Intent
		Intent intent = getIntent();
		String action = intent.getAction();
		String type = intent.getType();

		String accountUUID = "";

		boolean multiAccount = true;

		if (savedInstanceState != null) {
			if(savedInstanceState.containsKey(EXTRA_ACCOUNT_UUID)) {
				accountUUID = savedInstanceState.getString(EXTRA_ACCOUNT_UUID);
				mPreserveAccount=true;
			}
			if(savedInstanceState.containsKey(EXTRA_ACTIVITY)) {
				mActivityId = savedInstanceState.getString(EXTRA_ACTIVITY);
				mAction = savedInstanceState.getInt(EXTRA_ACTION);
				multiAccount = false;
			}
		} else if (extras != null) {
			if(extras.containsKey(EXTRA_ACCOUNT_UUID)) {
				accountUUID = extras.getString(EXTRA_ACCOUNT_UUID);
				mPreserveAccount=true;
			}
			if(extras.containsKey(EXTRA_ACTIVITY)) {
				mActivityId = extras.getString(EXTRA_ACTIVITY);
				mAction = extras.getInt(EXTRA_ACTION);
				multiAccount = false;
			}
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


		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mSettings = getSharedPreferences(K_PUMA_SETTINGS,Context.MODE_PRIVATE);

		mTitle = (EditText)findViewById(R.id.title);
		mNote = (EditText)findViewById(R.id.note);
		mTextViewFileName = (TextView)findViewById(R.id.filename);

		mCheckBoxLocation = (CheckBox)findViewById(R.id.enable_location);
		handleGeoCheckbox();

		mCheckBoxPublic = (CheckBox)findViewById(R.id.public_post);
		handlePublicCheckbox();

		if(multiAccount) {
			mAccounts = am.getAccounts();
			if(mAccounts.size() == 1) {
				multiAccount= false;
			}
		}

		if(multiAccount) {
			setAccountsSpinner();
		} else {
			hideSpinner();
		}

		mNotificationManager.cancel(0);

		if (Intent.ACTION_SEND.equals(action) && type != null) {
			if (type.equals("text/plain")) {
				handleSendText(intent); // Handle text, link etc.
			} else if(type.startsWith("image/")) {
				handleSendImage(intent);
			}
			// add else if for other types (images etc.)
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putString(EXTRA_ACTIVITY, mActivityId);
		outState.putInt(EXTRA_ACTION, mAction);
		if(mPreserveAccount) {
			outState.putString(EXTRA_ACCOUNT_UUID, mAccount.getUuid());
		}
		super.onSaveInstanceState(outState);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.compose, menu);
		if(mAction == ACTION_REPLY) {
			menu.removeItem(R.id.action_add_picture);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_post:
			onPostAction();
			return true;

		case R.id.action_add_picture:
			showFileChooser();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void showFileChooser() {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("image/*");
		startActivityForResult(intent, CHOOSE_FILE_ID);
	}

	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);

		if (requestCode == CHOOSE_FILE_ID) {
			if (resultCode == RESULT_OK) {
				Uri uri = intent.getData();
				if (uri != null) {
					Log.d(APP_NAME,"mUri: " + uri);
					setFileFromUri(uri);
				} 
			} 

		}
	}

	private boolean setFileFromUri(Uri uri) {
		boolean isFile = false;
		String[] projection = { MediaStore.Images.Media.DATA };
		Cursor cursor = getContentResolver().query(uri, projection, null, null,null);
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				mFilename = new File( cursor.getString(cursor
						.getColumnIndexOrThrow(ImageColumns.DATA)));

				if(mFilename != null && mFilename.exists() && mFilename.canRead()) {
					Log.d(APP_NAME,"mFilename: " + mFilename.getAbsolutePath());
					onAddedPicture();
					isFile=true;
				} else {
					Toast.makeText(mContext, "There's a problem accessing this image...", Toast.LENGTH_SHORT).show();
				}

			}
			cursor.close();
			//			mTextViewFileName.setText(mFilename.getName());
		}
		if(!isFile) {
			onRemovedPicture();
		}
		return isFile;
	}

	private void onAddedPicture() {
		mTextViewFileName.setText(mFilename.getName());
		findViewById(R.id.ll_attachment).setVisibility(View.VISIBLE);
		findViewById(R.id.ll_title).setVisibility(View.VISIBLE);
	}

	private void onRemovedPicture() {
		mFilename = null;
		mTextViewFileName.setText("");
		findViewById(R.id.ll_attachment).setVisibility(View.GONE);
		findViewById(R.id.ll_title).setVisibility(View.GONE);
	}

	void handleSendText(Intent intent) {
		String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
		if (sharedText != null) {
			Log.d(APP_NAME, "Shared text: " + sharedText);
			// Update UI to reflect text being shared
			mNote = (EditText) findViewById(R.id.note);
			mNote.setText(sharedText);
		}
	}

	void handleSendImage(Intent intent) {
		Uri uri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
		if (uri != null) {
			setFileFromUri(uri);
		}
	}

	private void postComplete() {
		broadcastIntentReload();
	}

	private void postFailed() {
		Toast.makeText(this, R.string.post_failed, Toast.LENGTH_SHORT).show();
	}

	private static class PostHandler extends Handler {

		private final WeakReference<ComposeActivity> mTarget;

		private static final int MSG_POST_OK = 0;
		private static final int MSG_POST_KO = 1;

		PostHandler(ComposeActivity target) {
			mTarget = new WeakReference<ComposeActivity>(target);
		}

		public void handleMessage(Message msg) {
			ComposeActivity target = mTarget.get();

			switch (msg.what) {

			case MSG_POST_OK:
				target.postComplete();
				break;

			case MSG_POST_KO:
				target.postFailed();
				break;
			}
		}

		void sendPostComplete() {
			sendEmptyMessage(MSG_POST_OK);
		}

		void sendPostFailed() {
			sendEmptyMessage(MSG_POST_KO);
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
			//			Log.d(APP_NAME,"Spinner obj: " + o);
			current = findAccountByName((String)o);
		}

		//		String title = mTitle.getText().toString();
		String note = mNote.getText().toString();
		try {
			note = new Markdown4jProcessor().process(note);
		} catch(IOException e) {
			Log.e(APP_NAME,e.getMessage(),e);
		}
		String title = mTitle.getText().toString();

		Location location = null;
		if(mCheckBoxLocation.isChecked()) {
			LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
			location = LocationUtil.getMostRecentLastKnownLocation(locationManager);
		}
		JSONObject inReplyTo = null;
		switch(mAction) {
		case ACTION_REPLY:
			try {
				inReplyTo = new JSONObject(mActivityId);
			} catch(JSONException e) {
				Log.e(APP_NAME,e.getMessage(),e);
			}
			break;


		default:
			// 
		}

		post(current, title,  note, inReplyTo, mCheckBoxPublic.isChecked(), location);

	}

	private void post(final Account account, final String title,  final String note, final JSONObject inReplyTo, final boolean isPublicNote, final Location location ) {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				Pumpio pumpio = new Pumpio(mContext);
				pumpio.setAccount(account);
				boolean posted = false;
				if(mFilename != null) {

					try {

						BitmapFactory.Options options = new BitmapFactory.Options();
						options.inJustDecodeBounds = true;
						BitmapFactory.decodeFile(mFilename.getAbsolutePath(),options);
						int imageHeight = options.outHeight;
						int imageWidth = options.outWidth;
						String imageType = options.outMimeType;

						Log.d(APP_NAME,"imageHeight: " + imageHeight);
						Log.d(APP_NAME,"imageWidth: " + imageWidth);
						Log.d(APP_NAME,"imageType: " + imageType);

						if(imageHeight > 1024 || imageWidth > 1024) {
							// Calculate inSampleSize
							options.inSampleSize = ImageUtil.calculateInSampleSize(options, 1024, 1024);
						}
						// Decode bitmap with inSampleSize set
						options.inJustDecodeBounds = false;
						Bitmap bm = BitmapFactory.decodeFile(mFilename.getAbsolutePath(), options);

						ByteArrayOutputStream out = new ByteArrayOutputStream(10240);
						bm.compress(CompressFormat.JPEG, 100, out);

						posted = pumpio.postImage(title, note, isPublicNote, location, imageType, out.toByteArray());
					} catch (Exception e) {
						Log.e(APP_NAME,e.getMessage(),e);
						mHandler.sendPostFailed();
					}
				} else {
					try {
						posted = pumpio.postNote(inReplyTo, note, isPublicNote, location);
					} catch (Exception e) {
						Log.e(APP_NAME,e.getMessage(),e);
						mHandler.sendPostFailed();
					}
				}
				if(posted) {
					mHandler.sendPostComplete();
				} else {
					mHandler.sendPostFailed();
				}
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

	public static void startActivity(Context context,Account account, String activityId, int action) {
		Intent homeIntent = new Intent(context,ComposeActivity.class);
		homeIntent.putExtra(ComposeActivity.EXTRA_ACCOUNT_UUID, account.getUuid());
		homeIntent.putExtra(ComposeActivity.EXTRA_ACTIVITY, activityId);
		homeIntent.putExtra(ComposeActivity.EXTRA_ACTION, action);

		context.startActivity(homeIntent);
	}

	public static void startActivity(Context context) {
		Intent homeIntent = new Intent(context,ComposeActivity.class);
		context.startActivity(homeIntent);
	}

	private void broadcastIntentReload() {
		Intent intent = new Intent(ACTION_ACTIVITY_POSTED);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}

}
