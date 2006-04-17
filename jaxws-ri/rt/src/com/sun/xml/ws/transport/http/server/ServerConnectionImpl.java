/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License).  You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the license at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * 
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.xml.ws.transport.http.server;

import com.sun.istack.NotNull;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.server.WebServiceContextDelegate;
import com.sun.xml.ws.transport.http.WSHTTPConnection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.ws.handler.MessageContext;


/**
 * {@link WSHTTPConnection} used with Java SE endpoints. It provides connection
 * implementation using {@link HttpExchange} object.
 *
 * @author Jitendra Kotamraju
 */
final class ServerConnectionImpl extends WSHTTPConnection implements WebServiceContextDelegate {

    private final HttpExchange httpExchange;
    private int status;
    private int responseContentLength = 0;

    private boolean outputWritten;


    public ServerConnectionImpl(@NotNull HttpExchange httpExchange) {
        this.httpExchange = httpExchange;
    }

    @Override
    @Property(MessageContext.HTTP_REQUEST_HEADERS)
    public @NotNull Map<String,List<String>> getRequestHeaders() {
        return httpExchange.getRequestHeaders();
    }

    @Override
    public String getRequestHeader(String headerName) {
        return httpExchange.getRequestHeaders().getFirst(headerName);
    }

    @Override
    @Property(MessageContext.HTTP_RESPONSE_HEADERS)
    public void setResponseHeaders(Map<String,List<String>> headers) {
        Headers r = httpExchange.getResponseHeaders();
        r.clear();
        for(Map.Entry <String, List<String>> entry : headers.entrySet()) {
            String name = entry.getKey();
            List<String> values = entry.getValue();
            if (name.equalsIgnoreCase("Content-Length")) {
                // No need to add this header
                responseContentLength = Integer.parseInt(values.get(0));
            } else {
                r.put(name,new ArrayList<String>(values));
            }
        }
    }

    @Override
    public void setContentTypeResponseHeader(@NotNull String value) {
        httpExchange.getResponseHeaders().set("Content-Type",value);
    }

    @Override
    @Property(MessageContext.HTTP_RESPONSE_CODE)
    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    @Property(MessageContext.HTTP_RESPONSE_CODE)
    public int getStatus() {
        return status;
    }

    public @NotNull InputStream getInput() {
        return httpExchange.getRequestBody();
    }

    public @NotNull OutputStream getOutput() throws IOException {
        assert !outputWritten;
        outputWritten = true;

        httpExchange.sendResponseHeaders(getStatus(), responseContentLength);

        return httpExchange.getResponseBody();
    }

    public @NotNull WebServiceContextDelegate getWebServiceContextDelegate() {
        return this;
    }

    public Principal getUserPrincipal(Packet request) {
        return null;
    }

    public boolean isUserInRole(Packet request, String role) {
        return false;
    }

    @Override
    @Property(MessageContext.HTTP_REQUEST_METHOD)
    public @NotNull String getRequestMethod() {
        return httpExchange.getRequestMethod();
    }

    @Override
    @Property(MessageContext.QUERY_STRING)
    public String getQueryString() {
        URI requestUri = httpExchange.getRequestURI();
        String query = requestUri.getQuery();
        if (query != null)
            return query;
        return null;
    }

    @Override
    @Property(MessageContext.PATH_INFO)
    public String getPathInfo() {
        URI requestUri = httpExchange.getRequestURI();
        String reqPath = requestUri.getPath();
        String ctxtPath = httpExchange.getHttpContext().getPath();
        if (reqPath.length() > ctxtPath.length()) {
            return reqPath.substring(ctxtPath.length());
        }
        return null;
    }

    protected PropertyMap getPropertyMap() {
        return model;
    }

    private static final PropertyMap model;

    static {
        model = parse(ServerConnectionImpl.class);
    }
}
