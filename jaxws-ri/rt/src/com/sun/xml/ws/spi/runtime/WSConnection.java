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
package com.sun.xml.ws.spi.runtime;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.sandbox.server.WebServiceContextDelegate;
import com.sun.xml.ws.util.PropertySet.Property;

import javax.servlet.http.HttpServletRequest;
import javax.xml.ws.handler.MessageContext;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;


/**
 * The view of an HTTP exchange from the point of view of JAX-WS.
 *
 * <p>
 * Different HTTP server layer uses different implementations of this class
 * so that JAX-WS can be shielded from individuality of such layers.
 */
public interface WSConnection {
    
    public static final int OK=200;
    public static final int ONEWAY=202;
    public static final int UNSUPPORTED_MEDIA=415;
    public static final int MALFORMED_XML=400;
    public static final int INTERNAL_ERR=500;

    /**
     * This is an opportunity for {@link WSConnection} to fill in a {@link Packet}
     * additional transport-specific properties.
     */
    void wrapUpRequestPacket(Packet p);

    /**
     * sets transport headers
     */
    void setResponseHeaders(Map<String,List<String>> headers);
    
    /**
     * sets the transport status code like <code>OK</code>
     */
    void setStatus(int status);

    /**
     * Transport's underlying input stream
     * @return Transport's underlying input stream
     */
    InputStream getInput();
    
    /**
     * Closes transport's input stream
     */
    void closeInput();
    
    /**
     * Transport's underlying output stream
     * @return Transport's underlying output stream
     */
    OutputStream getOutput();
    
    /**
     * Closes transport's output stream
     */
    void closeOutput();

    /**
     * Closes transport connection
     */
    void close();

    /**
     * Returns the {@link WebServiceContextDelegate} for this connection.
     */
    WebServiceContextDelegate getWebServiceContextDelegate();

    /**
     * HTTP request method, such as "GET" or "POST".
     */
    @Property(javax.xml.ws.handler.MessageContext.HTTP_REQUEST_METHOD)
    String getRequestMethod();

    /**
     * HTTP request headers.
     *
     * @deprecated
     *      This is a potentially expensive operation.
     *      Programs that want to access HTTP headers should consider using
     *      other methods.
     *
     * @return
     *      can be empty but never null.
     */
    @Property(MessageContext.HTTP_REQUEST_HEADERS)
    Map<String, List<String>> getRequestHeaders();

    /**
     * Gets an HTTP request header.
     *
     * <p>
     * if multiple headers are present, this method returns one of them.
     * (The implementation is free to choose which one it returns.)
     *
     * @return
     *      null if no header exists.
     */
    String getRequestHeader(String headerName);

    /**
     * HTTP Query string, such as "foo=bar", or null if none exists.
     */
    @Property(MessageContext.QUERY_STRING)
    String getQueryString();

    /**
     * Requested path. A string like "/foo/bar/baz".
     *
     * @see HttpServletRequest#getPathInfo()
     */
    @Property(MessageContext.PATH_INFO)
    String getPathInfo();
}
