package org.macno.puma.manager;

import static org.macno.puma.PumaApplication.APP_NAME;
import static org.macno.puma.PumaApplication.K_ACCOUNT_SETTINGS;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;
import org.macno.puma.core.Account;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class AccountManager {

	private static final String K_DEFAULT_ACCOUNT = "DefaultAccount";

	private SharedPreferences mSettings;
	
	public AccountManager(Context context) {
		mSettings = context.getSharedPreferences(K_ACCOUNT_SETTINGS,Context.MODE_PRIVATE);
	}

	public Account getDefaultAccount() {
		String accountUUID = mSettings.getString(K_DEFAULT_ACCOUNT, null);
		if (accountUUID == null) {
			return null;
		}
		String json = mSettings.getString(accountUUID, null);
		if (json == null) {
			return null;
		}
		Account account = null;
		try {
			JSONObject jaccount = new JSONObject(json);
			account = Account.fromJSON(jaccount);
		} catch(JSONException e) {
			Log.e(APP_NAME, "Error creating account json string: " + e.toString());
		}
		return account;
	}

	public Account getAccount(String uuid) {
		String json = mSettings.getString(uuid, null);
		if (json == null) {
			return null;
		}
		Account account = null;
		try {
			JSONObject jaccount = new JSONObject(json);
			account = Account.fromJSON(jaccount);
		} catch(JSONException e) {
			Log.e(APP_NAME, "Error creating account json string: " + e.toString());
		}
		return account;
	}
	
	/**
	 * 
	 * @return all existing accounts
	 */
	public ArrayList<Account> getAccounts() {
		ArrayList<Account> accounts = new ArrayList<Account>();
		Set<String> keys = mSettings.getAll().keySet();
		for(String key : keys) {
			if(key.equals(K_DEFAULT_ACCOUNT))
				continue;
			String json = mSettings.getString(key, null);
			try {
				JSONObject jaccount = new JSONObject(json);
				Account account = Account.fromJSON(jaccount);
				accounts.add(account);
			} catch(JSONException e) {
				Log.e(APP_NAME, "Error creating account json string: " + e.toString());
			}
		}
		return accounts;
	}
	
	public Account create(String username, String node) {
		UUID accountUUID = UUID.randomUUID();
		Account account = new Account();
		account.setUuid(accountUUID.toString());
		account.setUsername(username);
		account.setNode(node);
		mSettings.edit().putString(account.getUuid(), account.toString()).commit();
		return account;
	}
	
	public boolean save(Account account) {
		return mSettings.edit().putString(account.getUuid(), account.toString()).commit();
	}
	
	/**
	 * Remove a account
	 * 
	 * @param account
	 * @return
	 */
	public boolean delete(Account account) {
		String accountUUID = mSettings.getString(K_DEFAULT_ACCOUNT, null);
		if (accountUUID != null && account.getUuid().equals(accountUUID)) {
			resetDefault();
		}
		return mSettings.edit().remove(account.getUuid()).commit();
	}
	
	/**
	 * Set the default account
	 * 
	 * @param account
	 */
	public void setDefault(Account account) {
		mSettings.edit().putString(K_DEFAULT_ACCOUNT,account.getUuid()).commit();
	}
	
	public void resetDefault() {
		mSettings.edit().remove(K_DEFAULT_ACCOUNT).commit();
	}
}
