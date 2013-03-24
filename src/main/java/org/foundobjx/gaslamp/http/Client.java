package org.foundobjx.gaslamp.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;

public class Client {
	public InputStream get(final RequestVO request) throws IOException {
		HttpURLConnection connection =  (HttpURLConnection)request.getUrl().openConnection();
		connection.setRequestMethod("GET");
		initialize(request, connection);
		return connection.getInputStream();
	}
	public InputStream post(final RequestVO request) throws IOException {
		HttpURLConnection connection =  (HttpURLConnection)request.getUrl().openConnection();
		initialize(request, connection);
		connection.setRequestMethod("POST");
        final OutputStream out = connection.getOutputStream();
        out.write(request.getMessage().getBytes(), 0, request.getMessage().getBytes().length);
        out.flush();
        out.close();
		return connection.getInputStream();
	}

	private HttpURLConnection initialize(final RequestVO request, final HttpURLConnection connection) {
		connection.setConnectTimeout(request.getTimeout());
		connection.setReadTimeout(request.getTimeout());
		connection.setInstanceFollowRedirects(false);
		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setUseCaches(true);
		connection.setAllowUserInteraction(true);
        connection.setRequestProperty("Authorization", "Basic " + request.getAuth());
        return connection;
    }
}
