package org.macno.puma.core;

import org.json.JSONException;
import org.json.JSONObject;

public class Account {

	private static final String K_UUID = "uuid";
	private static final String K_USERNAME = "username";
	private static final String K_NODE = "node";
	private static final String K_SSL = "ssl";
	
	private static final String K_OAUTH_CLIENT_ID = "oauthClientId";
	private static final String K_OAUTH_CLIENT_SECRET = "oauthClientSecret";
	private static final String K_OAUTH_TOKEN = "oauthToken";
	private static final String K_OAUTH_TOKEN_SECRET = "oauthTokenSecret";
	
	private String uuid;
	private String username;
	private String node;
	private boolean ssl;
	private String oauthClientId;
	private String oauthClientSecret;
	private String oauthToken;
	private String oauthTokenSecret;
	
	
	public boolean isSsl() {
		return ssl;
	}

	public void setSsl(boolean ssl) {
		this.ssl = ssl;
	}

	public String getOauthClientId() {
		return oauthClientId;
	}

	public void setOauthClientId(String oauthClientId) {
		this.oauthClientId = oauthClientId;
	}

	public String getOauthClientSecret() {
		return oauthClientSecret;
	}

	public void setOauthClientSecret(String oauthClientSecret) {
		this.oauthClientSecret = oauthClientSecret;
	}

	public String getOauthToken() {
		return oauthToken;
	}

	public void setOauthToken(String oauthToken) {
		this.oauthToken = oauthToken;
	}

	public String getOauthTokenSecret() {
		return oauthTokenSecret;
	}

	public void setOauthTokenSecret(String oauthTokenSecret) {
		this.oauthTokenSecret = oauthTokenSecret;
	}

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

	public static Account fromJSON(JSONObject o) {
		Account account = new Account();
		account.setUsername(o.optString(K_USERNAME));
		account.setNode(o.optString(K_NODE));
		account.setUuid(o.optString(K_UUID));
		account.setSsl(o.optBoolean(K_SSL, true));
		account.setOauthClientId(o.optString(K_OAUTH_CLIENT_ID));
		account.setOauthClientSecret(o.optString(K_OAUTH_CLIENT_SECRET));
		account.setOauthToken(o.optString(K_OAUTH_TOKEN, null));
		account.setOauthTokenSecret(o.optString(K_OAUTH_TOKEN_SECRET, null));
		return account;
	}
	
	public JSONObject toJSON() {
		JSONObject ret = new JSONObject();
		try {
			ret
				.put(K_UUID, uuid)
				.put(K_USERNAME, username)
				.put(K_NODE, node)
				.put(K_SSL, ssl)
				.put(K_OAUTH_CLIENT_ID, oauthClientId)
				.put(K_OAUTH_CLIENT_SECRET, oauthClientSecret)
				.put(K_OAUTH_TOKEN, oauthToken)
				.put(K_OAUTH_TOKEN_SECRET, oauthTokenSecret);
		} catch (JSONException e) {
			
			e.printStackTrace();
		}
		return ret;
	}
	
	@Override
	public String toString() {
		return toJSON().toString();
	}
	
}
