package org.macno.puma.activity;

import org.macno.puma.R;
import org.macno.puma.adapter.StreamPageAdapter;
import org.macno.puma.core.Account;
import org.macno.puma.manager.AccountManager;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;

public class HomeActivity extends FragmentActivity {

	public static final String EXTRA_ACCOUNT_UUID = "extraAccountUUID";
	
	private Account mAccount;
	
	private StreamPageAdapter mAdapter;
	
	private ViewPager mPager;
	
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
		
		if(mAccount == null) {
			AccountAddActivity.startActivity(this);
			finish();
		}
		
		mPager = (ViewPager)findViewById(R.id.pager);
		
		mAdapter = new StreamPageAdapter(this,mAccount);
		mPager.setAdapter(mAdapter);

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
	        
	        case R.id.action_add_account:
	        	onAddAccountAction();
	        	return true;
	       
	        case R.id.action_compose:
	        	ComposeActivity.startActivity(this, mAccount);
	        	return true;
	        	
	        case R.id.action_refresh:
	        	onRefreshAction();
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
    
    private void onAddAccountAction() {
		AccountAddActivity.startActivity(this);
		finish();
    }
    
    private void onRefreshAction() {
    	mAdapter.refreshAdapter(mPager.getCurrentItem());
    }
    
    public static void startActivity(Context context,Account account) {
		Intent homeIntent = new Intent(context,HomeActivity.class);
		homeIntent.putExtra(HomeActivity.EXTRA_ACCOUNT_UUID, account.getUuid());
		context.startActivity(homeIntent);
		
	}
    
}
