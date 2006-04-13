package com.sun.xml.ws.transport.http.client;

import com.sun.xml.ws.util.PropertySet;
import com.sun.xml.ws.client.ResponseContext;
import com.sun.xml.ws.client.BindingProviderProperties;
import com.sun.istack.NotNull;

import javax.xml.ws.handler.MessageContext;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;
import java.io.IOException;

/**
 * Properties exposed from {@link HttpTransportPipe} for {@link ResponseContext}.
 *
 * @author Kohsuke Kawaguchi
 */
final class HttpResponseProperties extends PropertySet {

    private final HttpURLConnection con;

    public HttpResponseProperties(@NotNull HttpURLConnection con) {
        this.con = con;
    }

    @Property(MessageContext.HTTP_RESPONSE_HEADERS)
    public Map<String, List<String>> getResponseHeaders() {
        return con.getHeaderFields();
    }

    @Property(MessageContext.HTTP_RESPONSE_CODE)
    public int getResponseCode() {
        try {
            return con.getResponseCode();
        } catch (IOException e) {
            // by this time the connection should have been complete, so this is not possible
            throw new AssertionError(e);
        }
    }

    /**
     * Use {@link #getResponseCode()}.
     */
    @Deprecated
    @Property(BindingProviderProperties.HTTP_STATUS_CODE)
    public int getStatusCode() {
        return getResponseCode();
    }


    @Override
    protected PropertyMap getPropertyMap() {
        return model;
    }

    private static final PropertyMap model;

    static {
        model = parse(HttpResponseProperties.class);
    }
}
