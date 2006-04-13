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
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.server.WebServiceContextDelegate;
import com.sun.xml.ws.transport.WSConnectionImpl;
import com.sun.xml.ws.util.ByteArrayBuffer;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.Principal;


/**
 * Server-side Local transport implementation
 *
 * @author WS Development Team
 */
final class LocalConnectionImpl extends WSConnectionImpl implements WebServiceContextDelegate {
    private ByteArrayBuffer baos;

    public LocalConnectionImpl() {
    }

    public InputStream getInput () {
        return baos.newInputStream();
    }

    public OutputStream getOutput () {
        baos = new ByteArrayBuffer();
        return baos;
    }

    public String toString() {
        return baos.toString();
    }

    public WebServiceContextDelegate getWebServiceContextDelegate() {
        return this;
    }

    public Principal getUserPrincipal(Packet request) {
        return null;   // not really supported
    }

    public boolean isUserInRole(Packet request, String role) {
        return false;   // not really supported
    }

    public String getRequestMethod() {
        return "POST";   // not really supported
    }

    public String getQueryString() {
        return null;   // not really supported
    }

    public String getPathInfo() {
        return null;   // not really supported
    }

    protected PropertyMap getPropertyMap() {
        return model;
    }

    private static final PropertyMap model;

    static {
        model = parse(LocalConnectionImpl.class);
    }
}

