package com.figaf.plugin.entities;

import org.apache.http.client.methods.HttpRequestBase;

import java.net.URI;

/**
 * @author Nesterov Ilya
 */
public class HttpUnlock extends HttpRequestBase {

    public final static String METHOD_NAME = "UNLOCK";

    public HttpUnlock() {
        super();
    }

    public HttpUnlock(final URI uri) {
        super();
        setURI(uri);
    }

    public HttpUnlock(final String uri) {
        super();
        setURI(URI.create(uri));
    }

    @Override
    public String getMethod() {
        return METHOD_NAME;
    }
}
