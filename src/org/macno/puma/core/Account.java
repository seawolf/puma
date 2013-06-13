package org.macno.puma.core;

import org.json.JSONException;
import org.json.JSONObject;

public class Account {

	private static final String K_UUID = "uuid";
	private static final String K_USERNAME = "username";
	private static final String K_NODE = "node";
	
	private String uuid;
	private String username;
	private String node;
	
	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getNode() {
		return node;
	}

	public void setNode(String node) {
		this.node = node;
	}

	public static Account fromJSON(JSONObject account) {
		Account a = new Account();
		try {
			if(account.has(K_USERNAME)) {
				a.setUsername(account.getString(K_USERNAME));
			}
			if(account.has(K_NODE)) {
				a.setNode(account.getString(K_NODE));
			}
			if(account.has(K_UUID)) {
				a.setNode(account.getString(K_UUID));
			}
		} catch (JSONException e) {
			
			e.printStackTrace();
		}
		return a;
	}
	
	public JSONObject toJSON() {
		JSONObject ret = new JSONObject();
		try {
			ret
				.put(K_UUID, uuid)
				.put(K_USERNAME, username)
				.put(K_NODE, node);
		} catch (JSONException e) {
			
			e.printStackTrace();
		}
		return ret;
	}
}
