package org.foundobjx.gaslamp.http;

import java.net.URL;

public class RequestBuilder extends RequestBuilderBase<RequestBuilder> {
	public static RequestBuilder request() {
		return new RequestBuilder();
	}

	public RequestBuilder() {
		super(new RequestVO());
	}

	public RequestVO build() {
		return getInstance();
	}
}

class RequestBuilderBase<T extends RequestBuilderBase<T>> {
	private RequestVO instance;

	protected RequestBuilderBase(RequestVO aInstance) {
		instance = aInstance;
	}

	protected RequestVO getInstance() {
		return instance;
	}

	public T withMessage(String aValue) {
		instance.setMessage(aValue);
		return cast();
	}

	public T withAuth(String aValue) {
		instance.setAuth(aValue);
		return cast();
	}

	public T withUrl(URL aValue) {
		instance.setUrl(aValue);
		return cast();
	}

	public T withTimeout(final int timeout) {
		instance.setTimeout(timeout);
		return cast();
	}
	
	@SuppressWarnings("unchecked")
	private <T> T cast() {
        return (T) this;
	}
}
