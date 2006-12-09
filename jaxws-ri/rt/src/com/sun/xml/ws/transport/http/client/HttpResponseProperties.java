package com.sun.xml.ws.transport.http.client;

import com.sun.istack.NotNull;
import com.sun.xml.ws.api.PropertySet;
import com.sun.xml.ws.client.BindingProviderProperties;
import com.sun.xml.ws.client.ResponseContext;

import javax.xml.ws.handler.MessageContext;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

/**
 * Properties exposed from {@link HttpTransportPipe} for {@link ResponseContext}.
 *
 * @author Kohsuke Kawaguchi
 */
final class HttpResponseProperties extends PropertySet {

    private final HttpClientTransport deferedCon;

    public HttpResponseProperties(@NotNull HttpClientTransport con) {
        this.deferedCon = con;
    }

    @Property(MessageContext.HTTP_RESPONSE_HEADERS)
    public Map<String, List<String>> getResponseHeaders() {
        return deferedCon.getHeaders();
    }

    @Property(MessageContext.HTTP_RESPONSE_CODE)
    public int getResponseCode() {
        return deferedCon.statusCode;
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
