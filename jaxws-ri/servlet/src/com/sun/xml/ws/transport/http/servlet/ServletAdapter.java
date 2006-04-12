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

import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.transport.http.WSHTTPConnection;
import com.sun.xml.ws.api.server.WSEndpoint;
import com.sun.xml.ws.transport.http.HttpAdapter;
import com.sun.xml.ws.transport.http.HttpAdapterList;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import java.io.IOException;

/**
 * {@link HttpAdapter} for servlets.
 *
 * <p>
 * This is a thin wrapper around {@link HttpAdapter} with some description
 * specified in the deployment (in particular those information are related
 * to how a request is routed to a {@link ServletAdapter}.
 */
public final class ServletAdapter extends HttpAdapter {
    final String name;
    /**
     * Servlet URL pattern with which this {@link HttpAdapter} is associated.
     */
    final String urlPattern;


    protected ServletAdapter(String name, String urlPattern, WSEndpoint endpoint, HttpAdapterList<ServletAdapter> owner) {
        super(endpoint, owner);
        this.name = name;
        this.urlPattern = urlPattern;
    }

    /**
     * Gets the name of the endpoint as given in the <tt>sun-jaxws.xml</tt>
     * deployment descriptor.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the "/abc/def/ghi" portion if
     * the URL pattern is "/abc/def/ghi/*".
     */
    public String getValidPath() {
        if (urlPattern.endsWith("/*")) {
            return urlPattern.substring(0, urlPattern.length() - 2);
        } else {
            return urlPattern;
        }
    }

    /**
     * Convenient method to return a port name from {@link WSEndpoint}.
     *
     * @return
     *      null if {@link WSEndpoint} isn't tied to any paritcular port.
     */
    public QName getPortName() {
        WSDLPort port = getEndpoint().getPort();
        if(port==null)  return null;
        else            return port.getName();
    }

    /**
     * Version of {@link #handle(WSConnection)}
     * that takes convenient parameters for servlet.
     */
    public void handle(ServletContext context, HttpServletRequest request, HttpServletResponse response) throws IOException {
        WSHTTPConnection connection = new ServletConnectionImpl(context,request,response);
        super.handle(connection);
    }

    /**
     * Version of {@link #publishWSDL(WSConnection, String, String)}
     * that takes convenient parameters for servlet.
     */
    public void publishWSDL(ServletContext context, HttpServletRequest request, HttpServletResponse response) throws IOException {
        WSHTTPConnection con = new ServletConnectionImpl(context,request,response);
        super.publishWSDL(con,getBaseAddress(request),request.getQueryString());
    }

    /**
     * Computes the base URL of the given request.
     */
    private String getBaseAddress(HttpServletRequest request) {
        StringBuilder addrBuf = new StringBuilder();
        addrBuf.append(request.getScheme());
        addrBuf.append("://");
        addrBuf.append(request.getServerName());
        addrBuf.append(':');
        addrBuf.append(request.getServerPort());
        addrBuf.append(request.getContextPath());
        //addrBuf.append(getValidPath());

        return addrBuf.toString();
    }

    ;

}
