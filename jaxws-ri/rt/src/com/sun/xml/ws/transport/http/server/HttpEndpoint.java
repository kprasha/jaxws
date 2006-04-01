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

package com.sun.xml.ws.transport.http.server;

import com.sun.net.httpserver.HttpContext;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.server.WSEndpoint;
import com.sun.xml.ws.transport.http.HttpAdapter;
import com.sun.xml.ws.server.ServerRtException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Hides {@link HttpContext} so that {@link EndpointImpl}
 * may load even without {@link HttpContext}.
 *
 * TODO: But what's the point? If Light-weight HTTP server isn't present,
 * all the publish operations will fail way. Why is it better to defer
 * the failure, as opposed to cause the failure as earyl as possible? -KK
 *
 * @author Jitendra Kotamraju
 */
final class HttpEndpoint {
    private String address;
    private HttpContext httpContext;
    private final HttpAdapter adapter;
    private final Executor executor;

    public HttpEndpoint(WSEndpoint endpoint, Executor executor) {
        this.executor = executor;
        /*
        Map<String, String> addressMap = null;
        WSDLPort port = endpoint.getPort();
        if (port != null) {
            addressMap = new HashMap<String, String>();
            // Maps port --> urlPattern. There is no url pattern for SE Endpoint
            addressMap.put(port.getName().getLocalPart(), "");
        }
         */
        this.adapter = new HttpAdapter(endpoint, null);
    }

    public void publish(String address) {
        this.address = address;
        httpContext = ServerMgr.getInstance().createContext(address);
        publish(httpContext);
    }

    public void publish(Object serverContext) {
        if (!(serverContext instanceof HttpContext)) {
            throw new ServerRtException("not.HttpContext.type", serverContext.getClass());
        }

        this.httpContext = (HttpContext)serverContext;
        publish(httpContext);
    }

    public void stop() {
        if (address == null) {
            // Application created its own HttpContext
            // httpContext.setHandler(null);
            httpContext.getServer().removeContext(httpContext);
        } else {
            // Remove HttpContext created by JAXWS runtime 
            ServerMgr.getInstance().removeContext(httpContext);
        }

        // Invoke WebService Life cycle method
        adapter.getEndpoint().dispose();
    }

    private void publish (HttpContext context) {
        context.setHandler(new WSHttpHandler(adapter, executor));
    }
    
    /*
    public static class EnpointAdapters extends HttpAdapters<HttpAdapter> {
        Map<String, String> addressMap = new HashMap<String, String>();
        
        public HttpAdapter createAdapter(String name, String urlPattern, WSEndpoint<?> endpoint, Class implementorClass) {

            ServletAdapter adapter = new ServletAdapter(name, urlPattern, endpoint, implementorClass, this);
            this.add(adapter);
            return adapter;

        }
        public Map<String, String> getPortAddresses() {
            if (addressMap.isEmpty()) {
                for(ServletAdapter adapter : this) {
                    addressMap.put(adapter.getPortName().getLocalPart(), adapter.getValidPath());
                }
            }
            return addressMap;
        }
    };
    */

}
