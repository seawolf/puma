package org.macno.puma.activity;

import org.macno.puma.R;
import org.macno.puma.core.Account;
import org.macno.puma.manager.AccountManager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class HomeActivity extends Activity {

	public static final String EXTRA_ACCOUNT_UUID = "extraAccountUUID";
	
	private Account mAccount;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Bundle extras = getIntent().getExtras();

        String accountUUID = "";
		if (savedInstanceState != null) {
			accountUUID = savedInstanceState.getString(EXTRA_ACCOUNT_UUID);
			
		} else if (extras != null) {
			accountUUID = extras.getString(EXTRA_ACCOUNT_UUID);
			
		}
		AccountManager am = new AccountManager(this);
		mAccount = am.getAccount(accountUUID);
		TextView welcome = (TextView)findViewById(R.id.tv_welcome);
		welcome.setText(getString(R.string.welcome_user,mAccount.getUsername()));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }
    
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case R.id.action_logout:
	        	onLogoutAction();
	        	return true;
	        	
	       
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}

    private void onLogoutAction() {
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder
			.setMessage(R.string.confirm_logout)
			.setCancelable(false)
			.setPositiveButton(android.R.string.yes,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface xdialog, int id) {
						doLogout();
					}
				})
			.setNegativeButton(android.R.string.cancel,null)
			.create()
			.show();
    }
    
    private void doLogout() {
    	AccountManager am = new AccountManager(this);
    	am.delete(mAccount);
    	MainActivity.startActivity(this);
    	finish();
    }
    
    public static void startActivity(Context context,Account account) {
		Intent homeIntent = new Intent(context,HomeActivity.class);
		homeIntent.putExtra(HomeActivity.EXTRA_ACCOUNT_UUID, account.getUuid());
		context.startActivity(homeIntent);
		
	}
    
}
