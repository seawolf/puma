package org.macno.puma.activity;

import java.util.ArrayList;

import org.macno.puma.core.Account;
import org.macno.puma.manager.AccountManager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AccountManager accountManager = new AccountManager(this);
		Account account = accountManager.getDefaultAccount();
		if(account == null) {
			ArrayList<Account> accounts = accountManager.getAccounts();
			int n_accounts = accounts.size();
			switch(n_accounts) {
			case 0:
				// Nessun profilo
				// mostrare welcome
				startAccountAddActivity();
				break;
			case 1:
				// Un solo profilo
				// Avvio activity esercizi
				account = accounts.get(0);
				accountManager.setDefault(account);
				startHomeActivity(account);
				break;
			default:
				// Pi� di un profilo
				// Gli faccio scegliere quale.
				
			}
		} else {
			// Avvio activity esercizi
			startHomeActivity(account);
		}
	}

	private void startAccountAddActivity() {
		AccountAddActivity.startActivity(this);
		finish();
	}
	
	private void startHomeActivity(Account account) {
		HomeActivity.startActivity(this, account);
		finish();
	}
	
	public static void startActivity(Context context) {
		Intent mainIntent = new Intent(context,MainActivity.class);
		context.startActivity(mainIntent);
	}
    

}
