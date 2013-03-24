package org.foundobjx.gaslamp.http;

import java.net.MalformedURLException;
import java.net.URL;

public class RequestVO {
	private String message;
    private String auth;
    private URL url;
    private int timeout;
    
    public RequestVO() {
    	message = "";
    	auth = "";
    	try {
			url = new URL("http://localhost");
		} catch (MalformedURLException e) {
		}
    	timeout = 10000;
    }
    public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public String getAuth() {
		return auth;
	}
	public void setAuth(String auth) {
		this.auth = auth;
	}
	public URL getUrl() {
		return url;
	}
	public void setUrl(URL url) {
		this.url = url;
	}
	public int getTimeout() {
		return timeout;
	}
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}
}