package org.macno.puma.util;

public class HttpUtilException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8055399269064981465L;
	
	private int code;
	
	public HttpUtilException(int code, String message) {
		super(message);
		this.code=code;
	}
	
	public int getCode() {
		return code;
	}
}
