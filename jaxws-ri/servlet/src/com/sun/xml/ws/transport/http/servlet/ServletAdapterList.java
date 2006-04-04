package com.sun.xml.ws.transport.http.servlet;

import com.sun.xml.ws.transport.http.HttpAdapterList;
import com.sun.xml.ws.api.server.WSEndpoint;

/**
 * List (and a factory) of {@link ServletAdapter}.
 *
 * @author Jitu
 */
public class ServletAdapterList extends HttpAdapterList<ServletAdapter> {
    @Override
    protected ServletAdapter createHttpAdapter(String name, String urlPattern, WSEndpoint<?> endpoint) {
        return new ServletAdapter(name, urlPattern, endpoint, this);
    }
}
