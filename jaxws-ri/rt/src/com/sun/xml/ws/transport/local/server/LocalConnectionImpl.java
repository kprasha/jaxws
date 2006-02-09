/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 * 
 * You can obtain a copy of the license at
 * https://jwsdp.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL
 * HEADER in each file and include the License file at
 * https://jwsdp.dev.java.net/CDDLv1.0.html  If applicable,
 * add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your
 * own identifying information: Portions Copyright [yyyy]
 * [name of copyright owner]
 */

package com.sun.xml.ws.transport.local.server;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.sandbox.server.WebServiceContextDelegate;
import com.sun.xml.ws.transport.WSConnectionImpl;
import com.sun.xml.ws.util.ByteArrayBuffer;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.Principal;
import java.util.List;
import java.util.Map;


/**
 * @author WS Development Team
 *
 * Server-side Local transport implementation
 */
public final class LocalConnectionImpl extends WSConnectionImpl implements WebServiceContextDelegate {
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
}

