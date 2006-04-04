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
package com.sun.xml.ws.transport.http.servlet;

import com.sun.xml.ws.api.BindingID;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.binding.BindingImpl;
import com.sun.xml.ws.api.server.WSConnection;
import com.sun.xml.ws.spi.runtime.WSRtObjectFactory;
import com.sun.xml.ws.transport.http.HttpAdapter;
import com.sun.xml.ws.util.xml.XmlUtil;
import org.xml.sax.EntityResolver;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URL;
import java.util.Collections;
import java.util.List;

/**
 * Singleton factory class to instantiate concrete objects.
 *
 * @author JAX-WS Development Team
 */
public class WSRtObjectFactoryImpl extends WSRtObjectFactory {

    /**
     * Creates a connection for servlet transport
     */
    @Override
    public WSConnection createWSConnection(
        ServletContext context, HttpServletRequest req, HttpServletResponse res ) {
        return new ServletConnectionImpl(context,req,res);
    }

    @Override
    public WSBinding createBinding(String bindingId) {
        return BindingImpl.create(BindingID.parse(bindingId));
    }

    /**
     * creates an EntityResolver for the XML Catalog URL
     */
    @Override
    public EntityResolver createEntityResolver(URL catalogUrl) {
        return XmlUtil.createEntityResolver(catalogUrl);
    }

    public List<HttpAdapter> getRuntimeEndpointInfos(ServletContext ctxt) {
        WSServletDelegate d = (WSServletDelegate) ctxt.getAttribute(
            WSServlet.JAXWS_RI_RUNTIME_INFO);

        if(d==null)
            return Collections.emptyList();
        else
            return Collections.<HttpAdapter>unmodifiableList(d.adapters);
    }
}
