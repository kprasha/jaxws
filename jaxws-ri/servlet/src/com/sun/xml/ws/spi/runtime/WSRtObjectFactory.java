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

import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.server.WSConnection;
import com.sun.xml.ws.transport.http.servlet.WSRtObjectFactoryImpl;
import com.sun.xml.ws.transport.http.HttpAdapter;
import org.xml.sax.EntityResolver;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.soap.SOAPBinding;
import javax.xml.ws.http.HTTPBinding;
import java.net.URL;
import java.util.List;

/**
 * Singleton abstract factory used to produce JAX-WS runtime related objects.
 */
public abstract class WSRtObjectFactory {

    private static final WSRtObjectFactory factory = new WSRtObjectFactoryImpl();

    /**
     * Obtain an instance of a factory. Don't worry about synchronization(at
     * the most, one more factory object is created).
     *
     */
    public static WSRtObjectFactory newInstance() {
        return factory;
    }
    
    /**
     * Creates a connection for servlet transport
     */
    public abstract WSConnection createWSConnection(
            ServletContext context, HttpServletRequest req, HttpServletResponse res);
    
    /**
     * @return List of endpoints
     */
    public abstract List<HttpAdapter> getRuntimeEndpointInfos(ServletContext ctxt);
    
    /**
     * creates the Binding object implementation.
     *
     * @param bindingId
     *      should be one of these values.
     *      {@link SOAPBinding#SOAP11HTTP_BINDING},
     *      {@link SOAPBinding#SOAP12HTTP_BINDING},
     *      {@link HTTPBinding#HTTP_BINDING}.
     */
    public abstract WSBinding createBinding(String bindingId);
    
    /**
     * creates an EntityResolver for the XML Catalog URL
     */
    public abstract EntityResolver createEntityResolver(URL catalogUrl);

}
