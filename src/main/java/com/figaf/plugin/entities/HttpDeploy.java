package com.figaf.plugin.entities;

import org.apache.http.client.methods.HttpRequestBase;

import java.net.URI;

/**
 * @author Nesterov Ilya
 */
public class HttpDeploy extends HttpRequestBase {

    public final static String METHOD_NAME = "DEPLOY";

    public HttpDeploy() {
        super();
    }

    public HttpDeploy(final URI uri) {
        super();
        setURI(uri);
    }

    public HttpDeploy(final String uri) {
        super();
        setURI(URI.create(uri));
    }

    @Override
    public String getMethod() {
        return METHOD_NAME;
    }
}
