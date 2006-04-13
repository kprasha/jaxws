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

package com.sun.xml.ws.transport.local;
import com.sun.istack.NotNull;
import com.sun.istack.Nullable;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.server.WebServiceContextDelegate;
import com.sun.xml.ws.transport.http.WSHTTPConnection;
import com.sun.xml.ws.util.ByteArrayBuffer;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * {@link WSHTTPConnection} implemented for the local transport.
 *
 * @author WS Development Team
 */
final class LocalConnectionImpl extends WSHTTPConnection implements WebServiceContextDelegate {

    private final Map<String, List<String>> reqHeaders;
    private Map<String, List<String>> rspHeaders = null;
    protected int statusCode;
    private ByteArrayBuffer baos;

    LocalConnectionImpl(@NotNull Map<String, List<String>> reqHeaders) {
        this.reqHeaders = reqHeaders;
    }

    public @NotNull InputStream getInput() {
        return baos.newInputStream();
    }

    public @NotNull OutputStream getOutput() {
        baos = new ByteArrayBuffer();
        return baos;
    }

    public String toString() {
        return baos.toString();
    }

    public @NotNull WebServiceContextDelegate getWebServiceContextDelegate() {
        return this;
    }

    public Principal getUserPrincipal(Packet request) {
        return null;   // not really supported
    }

    public boolean isUserInRole(Packet request, String role) {
        return false;   // not really supported
    }

    public @NotNull String getRequestMethod() {
        return "POST";   // not really supported
    }

    public String getQueryString() {
        return null;   // not really supported
    }

    public String getPathInfo() {
        return null;   // not really supported
    }

    public int getStatus () {
        return statusCode;
    }

    public void setStatus (int statusCode) {
        this.statusCode = statusCode;
    }

    public @Nullable Map<String, List<String>> getResponseHeaders() {
        return rspHeaders;
    }

    public @NotNull Map<String, List<String>> getRequestHeaders () {
        return reqHeaders;
    }

    public String getRequestHeader(String headerName) {
        List<String> values = getRequestHeaders().get(headerName);
        if(values==null || values.isEmpty())
            return null;
        else
            return values.get(0);
    }

    public void setResponseHeaders(Map<String,List<String>> headers) {
        this.rspHeaders = headers; // we are lazy
    }

    public void setContentTypeResponseHeader(@NotNull String value) {
        if(rspHeaders==null)
            rspHeaders = new HashMap<String,List<String>>();

        rspHeaders.put("Content-Type", Collections.singletonList(value));
    }



    protected PropertyMap getPropertyMap() {
        return model;
    }

    private static final PropertyMap model;

    static {
        model = parse(LocalConnectionImpl.class);
    }
}

