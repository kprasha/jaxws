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

import com.sun.net.httpserver.HttpExchange;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.server.WebServiceContextDelegate;
import com.sun.xml.ws.transport.WSConnectionImpl;
import com.sun.xml.ws.util.NoCloseInputStream;
import com.sun.xml.ws.util.NoCloseOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Map;


/**
 * <code>com.sun.xml.ws.api.server.WSConnection</code> used with Java SE endpoints
 *
 * @author WS Development Team
 */
final class ServerConnectionImpl extends WSConnectionImpl implements WebServiceContextDelegate {

    private HttpExchange httpExchange;
    private int status;
    private Map<String,List<String>> requestHeaders;
    private Map<String,List<String>> responseHeaders;
    private NoCloseInputStream is;
    private NoCloseOutputStream out;
    private boolean closedInput;
    private boolean closedOutput;

    public ServerConnectionImpl(HttpExchange httpTransaction) {
        this.httpExchange = httpTransaction;
    }

    public Map<String,List<String>> getHeaders() {
        return httpExchange.getRequestHeaders();
    }

    /**
     * sets response headers.
     */
    public void setResponseHeaders(Map<String,List<String>> headers) {
        responseHeaders = headers;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    /**
     * sets HTTP status code
     */
    public int getStatus() {
        if (status == 0) {
            status = HttpURLConnection.HTTP_INTERNAL_ERROR;
        }
        return status;
    }

    public InputStream getInput() {
        if (is == null) {
            is = new NoCloseInputStream(httpExchange.getRequestBody());
        }
        return is;
    }

    public OutputStream getOutput() {
        if (out == null) {
            try {
                closeInput();
                int len = 0;
                if (responseHeaders != null) {
                    for(Map.Entry <String, List<String>> entry : responseHeaders.entrySet()) {
                        String name = entry.getKey();
                        List<String> values = entry.getValue();
                        if (name.equals("Content-Length")) {
                            // No need to add this header
                            len = Integer.valueOf(values.get(0));
                        } else {
                            for(String value : values) {
                                httpExchange.getResponseHeaders().add(name, value);
                            }
                        }
                    }
                }

                // write HTTP status code, and headers
                httpExchange.sendResponseHeaders(getStatus(), len);
                out = new NoCloseOutputStream(httpExchange.getResponseBody());
            } catch(IOException ioe) {
                ioe.printStackTrace();
            }
        }
        return out;
    }

    public void closeOutput() {
        if (out != null) {
            try {
                out.doClose();
                closedOutput = true;
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        out = null;
    }

    public void closeInput() {
        if (is != null) {
            try {
                // Read everything from request and close it
                byte[] buf = new byte[1024];
                while (is.read(buf) != -1) {
                }
                is.doClose();
                closedInput = true;
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        is = null;
    }

    public void close() {
        try {
            if (!closedInput) {
                if (is == null) {
                    getInput();
                }
                closeInput();
            }
            if (!closedOutput) {
                if (out == null) {
                    getOutput();
                }
                closeOutput();
            }
        } finally {
            try {
                httpExchange.close();
            } catch(IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    public WebServiceContextDelegate getWebServiceContextDelegate() {
        return this;
    }

    public Principal getUserPrincipal(Packet request) {
        return null;
    }

    public boolean isUserInRole(Packet request, String role) {
        return false;
    }

    public String getRequestMethod() {
        return httpExchange.getRequestMethod();
    }

    public String getRequestHeader(String headerName) {
        return httpExchange.getRequestHeaders().getFirst(headerName);
    }

    public String getQueryString() {
        URI requestUri = httpExchange.getRequestURI();
        String query = requestUri.getQuery();
        if (query != null)
            return query;
        return null;
    }

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
