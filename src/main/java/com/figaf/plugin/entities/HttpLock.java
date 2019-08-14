package com.figaf.plugin.entities;

import org.apache.http.client.methods.HttpRequestBase;

import java.net.URI;

/**
 * @author Nesterov Ilya
 */
public class HttpLock extends HttpRequestBase {

    public final static String METHOD_NAME = "LOCK";

    public HttpLock() {
        super();
    }

    public HttpLock(final URI uri) {
        super();
        setURI(uri);
    }

    public HttpLock(final String uri) {
        super();
        setURI(URI.create(uri));
    }

    @Override
    public String getMethod() {
        return METHOD_NAME;
    }
}
