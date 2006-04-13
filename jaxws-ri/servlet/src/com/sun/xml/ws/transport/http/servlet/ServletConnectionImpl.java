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

package com.sun.xml.ws.transport.http.servlet;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.server.WebServiceContextDelegate;
import com.sun.xml.ws.transport.Headers;
import com.sun.xml.ws.transport.WSConnectionImpl;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.handler.MessageContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

/**
 * <code>com.sun.xml.ws.api.server.WSConnection</code> used by
 * WSServletDelegate, uses <code>javax.servlet.http.HttpServletRequest</code>
 * and <code>javax.servlet.http.HttpServletResponse</code>
 *
 * @author WS Development Team
 */
final class ServletConnectionImpl extends WSConnectionImpl implements WebServiceContextDelegate {

    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final ServletContext context;
    private int status;
    private Map<String,List<String>> requestHeaders;
    private Map<String,List<String>> responseHeaders;

    public ServletConnectionImpl(ServletContext context, HttpServletRequest request, HttpServletResponse response) {
        this.context = context;
        this.request = request;
        this.response = response;
    }

    @Override
    public Map<String,List<String>> getRequestHeaders() {
        if (requestHeaders == null) {
            requestHeaders = new Headers();
            Enumeration enums = request.getHeaderNames();
            while (enums.hasMoreElements()) {
                String headerName = (String) enums.nextElement();
                String headerValue = request.getHeader(headerName);
                List<String> values = requestHeaders.get(headerName);
                if (values == null) {
                    values = new ArrayList<String>();
                    requestHeaders.put(headerName, values);
                }
                values.add(headerValue);
            }
        }
        return requestHeaders;
    }

    /**
     * sets response headers.
     */
    @Override
    public void setResponseHeaders(Map<String,List<String>> headers) {
        responseHeaders = headers;
    }

    @Override
    public void setStatus(int status) {
        this.status = status;
    }

    /**
     * sets HTTP status code
     */
    @Override
    public int getStatus() {
        if (status == 0) {
            status = HttpURLConnection.HTTP_OK;
        }
        return status;
    }

    @Override
    public InputStream getInput() {
        try {
            return request.getInputStream();
        } catch(IOException ioe) {
            ioe.printStackTrace();
        }
        return null;
    }

    @Override
    public OutputStream getOutput() {
        // write HTTP status code, and headers
        response.setStatus(getStatus());
        if (responseHeaders != null) {
            for(Map.Entry <String, List<String>> entry : responseHeaders.entrySet()) {
                String name = entry.getKey();
                List<String> values = entry.getValue();
                for(String value : values) {
                    response.setHeader(name, value);
                }
            }
        }
        try {
            outputStream = response.getOutputStream();
            return outputStream;
        } catch(IOException ioe) {
            ioe.printStackTrace();
        }
        return null;
    }

    public WebServiceContextDelegate getWebServiceContextDelegate() {
        return this;
    }

    public Principal getUserPrincipal(Packet p) {
        return request.getUserPrincipal();
    }

    public boolean isUserInRole(Packet p,String role) {
        return request.isUserInRole(role);
    }

    @Override
    public String getRequestMethod() {
        return request.getMethod();
    }

    @Override
    public String getRequestHeader(String headerName) {
        return request.getHeader(headerName);
    }

    @Override
    public String getQueryString() {
        return request.getQueryString();
    }

    @Override
    public String getPathInfo() {
        return request.getPathInfo();
    }

    @Property(MessageContext.SERVLET_CONTEXT)
    public ServletContext getContext() {
        return context;
    }

    @Property(MessageContext.SERVLET_RESPONSE)
    public HttpServletResponse getResponse() {
        return response;
    }

    @Property(MessageContext.SERVLET_REQUEST)
    public HttpServletRequest getRequest() {
        return request;
    }

    protected PropertyMap getPropertyMap() {
        return model;
    }

    private static final PropertyMap model;

    static {
        model = parse(ServletConnectionImpl.class);
    }
}
