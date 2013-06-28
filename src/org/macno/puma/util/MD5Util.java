package org.macno.puma.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Util {

	private static String getHashString(MessageDigest digest) {
		StringBuilder builder = new StringBuilder();

		for (byte b : digest.digest()) {
			builder.append(Integer.toHexString((b >> 4) & 0xf));
			builder.append(Integer.toHexString(b & 0xf));
		}

		return builder.toString();
	}

	// MD5 hases are used to generate filenames based off a URL.
	public static String getMd5(String url) throws NoSuchAlgorithmException {
		
		MessageDigest mDigest = MessageDigest.getInstance("MD5");
		mDigest.update(url.getBytes());
		return getHashString(mDigest);
	
	}
}
