package com.sun.xml.ws.transport.http.servlet;

import com.sun.xml.ws.api.server.WSEndpoint;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.transport.http.HttpAdapter;
import com.sun.xml.ws.transport.http.DeploymentDescriptorParser.AdapterFactory;
import com.sun.xml.ws.api.server.InstanceResolver;
import com.sun.xml.ws.spi.runtime.WSConnection;

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
final class ServletAdapter extends HttpAdapter {
    final String name;
    /**
     * Servlet URL pattern with which this {@link HttpAdapter} is associated.
     */
    final String urlPattern;

    /**
     * The application class that ultimately implements the service.
     *
     * <p>
     * At {@link ServletAdapter} we require that there be a single
     * class that serves {@link InstanceResolver}.
     */
    final Class<?> implementationType;

    public ServletAdapter(String name, String urlPattern, WSEndpoint endpoint, Class<?> implementationType) {
        super(endpoint);
        this.name = name;
        this.urlPattern = urlPattern;
        this.implementationType = implementationType;
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
        WSConnection connection = new ServletConnectionImpl(context,request,response);
        super.handle(connection);
    }

    /**
     * Version of {@link #publishWSDL(WSConnection, String, String)}
     * that takes convenient parameters for servlet.
     */
    public void publishWSDL(ServletContext context, HttpServletRequest request, HttpServletResponse response) throws IOException {
        WSConnection con = new ServletConnectionImpl(context,request,response);
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
        addrBuf.append(getValidPath());

        return addrBuf.toString();
    }

    static final AdapterFactory<ServletAdapter> FACTORY = new AdapterFactory<ServletAdapter>() {
        public ServletAdapter createAdapter(String name, String urlPattern, WSEndpoint endpoint, Class implementorClass) {
            return new ServletAdapter(name,urlPattern,endpoint,implementorClass);
        }
    };
}
