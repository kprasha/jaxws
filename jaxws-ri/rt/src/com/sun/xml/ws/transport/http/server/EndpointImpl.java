
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

import com.sun.xml.stream.buffer.MutableXMLStreamBuffer;
import com.sun.xml.stream.buffer.XMLStreamBufferResult;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.server.WSEndpoint;
import com.sun.xml.ws.binding.BindingImpl;
import com.sun.xml.ws.api.server.InstanceResolver;
import com.sun.xml.ws.api.server.SDDocumentSource;
import com.sun.xml.ws.server.Root;
import com.sun.xml.ws.server.ServerRtException;
import com.sun.xml.ws.util.xml.XmlUtil;
import java.net.MalformedURLException;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.ws.Binding;
import javax.xml.ws.Endpoint;
import javax.xml.ws.WebServicePermission;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;


/**
 * Implements {@link Endpoint}.
 *
 * <p>
 * This class accumulates the information necessary to create
 * {@link WSEndpoint}, and then when {@link #publish} method
 * is called it will be created.
 *
 * <p>
 * This object also allows accumulated information to be retrieved.
 *
 * @author Jitendra Kotamraju
 */
public class EndpointImpl extends Endpoint {

    private static final WebServicePermission ENDPOINT_PUBLISH_PERMISSION =
        new WebServicePermission("publishEndpoint");

    /**
     * Once the service is published, this field will
     * be set to the {@link HttpEndpoint} instance.
     *
     * But don't declare the type as {@link HttpEndpoint}
     * to avoid static type dependency that cause the class loading to
     * fail if the LW HTTP server doesn't exist.
     */
    private Object actualEndpoint;

    // information accumulated for creating WSEndpoint
    private final WSBinding binding;
    private final Object implementor;
    private List<Source> metadata;
    private Executor executor;
    private Map<String,Object> properties = Collections.emptyMap(); // always non-null
    private boolean stopped;



    public EndpointImpl(String bindingId, Object impl) {
        binding = BindingImpl.getBinding(bindingId, impl.getClass(), null, false);
        implementor = impl;
    }

    /**
     * Wraps an already created {@link WSEndpoint} into an {@link EndpointImpl},
     * and immediately publishes it with the given context.
     *
     * @deprecated
     *      This is a backdoor method. Don't use it unless you know what you are doing.
     */
    public EndpointImpl(WSEndpoint wse, Object serverContext) {
        actualEndpoint = new HttpEndpoint(wse, executor);
        ((HttpEndpoint)actualEndpoint).publish(serverContext);
        binding = wse.getBinding();
        implementor = null; // this violates the semantics, but hey, this is a backdoor.
    }

    public Binding getBinding() {
        return binding;
    }

    public Object getImplementor() {
        return implementor;
    }
    
    public void publish(String address) {
        canPublish();
        URL url;
        try {
            url = new URL(address);
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException("Cannot create URL for this address "+address);
        }
        if (!url.getProtocol().equals("http")) {
            throw new IllegalArgumentException(url.getProtocol()+" protocol based address is not supported");
        }
        if (!url.getPath().startsWith("/")) {
            throw new IllegalArgumentException("Incorrect WebService address="+address+
                    ". The address's path should start with /");
        }
        createEndpoint();
        ((HttpEndpoint)actualEndpoint).publish(address);
    }

    public void publish(Object serverContext) {
        canPublish();
        if (!com.sun.net.httpserver.HttpContext.class.isAssignableFrom(serverContext.getClass())) {
            throw new IllegalArgumentException(serverContext.getClass()+" is not a supported context.");
        }
        createEndpoint();
        ((HttpEndpoint)actualEndpoint).publish(serverContext);
    }

    public void stop() {
        if (isPublished()) {
            ((HttpEndpoint)actualEndpoint).stop();
            actualEndpoint = null;
            stopped = true;
        }
    }

    public boolean isPublished() {
        return actualEndpoint!=null;
    }

    public List<Source> getMetadata() {
        return metadata;
    }

    public void setMetadata(java.util.List<Source> metadata) {
        if (isPublished()) {
            throw new IllegalStateException("Cannot set Metadata. Endpoint is already published");
        }
        this.metadata = metadata;
    }

    public Executor getExecutor() {
        return executor;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public Map<String,Object> getProperties() {
        return new HashMap<String, Object>(properties);
    }

    public void setProperties(Map<String, Object> map) {
        this.properties = new HashMap<String, Object>(map);
    }

    /*
    * Checks the permission of "publishEndpoint" before accessing HTTP classes.
    * Also it checks if there is an available HTTP server implementation.
    */
    private void createEndpoint() {
        // Checks permission for "publishEndpoint"
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(ENDPOINT_PUBLISH_PERMISSION);
        }

        // See if HttpServer implementation is available
        try {
            Class.forName("com.sun.net.httpserver.HttpServer");
        } catch(Exception e) {
            throw new UnsupportedOperationException("NOT SUPPORTED");
        }

        WSEndpoint wse = new Root().createSEIEndpoint(
            (Class) implementor.getClass(),
            InstanceResolver.createSingleton(implementor),
            getProperty(QName.class, Endpoint.WSDL_SERVICE),
            getProperty(QName.class, Endpoint.WSDL_PORT),
            null /* no container */,
            binding,
            null, /* TODO: how do we find a primary WSDL? */
            buildDocList(),
            null
        );
        // Don't load HttpEndpoint class before as it may load HttpServer classes
        actualEndpoint = new HttpEndpoint(wse, executor);
    }

    private <T> T getProperty(Class<T> type, String key) {
        Object o = properties.get(key);
        if(o==null)     return null;
        if(type.isInstance(o))
            return type.cast(o);
        else
            throw new IllegalArgumentException("Property "+key+" has to be of type "+type);   // i18n
    }

    /**
     * Convert metadata sources using identity transform. So that we can
     * reuse the Source object multiple times.
     */
    private List<SDDocumentSource> buildDocList() {
        List<SDDocumentSource> r = new ArrayList<SDDocumentSource>();

        if (metadata != null) {
            Transformer transformer = XmlUtil.newTransformer();
            for(Source source: metadata) {
                try {
                    XMLStreamBufferResult xsbr = new XMLStreamBufferResult();
                    transformer.transform(source, xsbr);
                    String systemId = source.getSystemId();

                    r.add(SDDocumentSource.create(new URL(systemId),xsbr.getXMLStreamBuffer()));
                } catch (TransformerException te) {
                    throw new ServerRtException("server.rt.err",te);
                } catch (IOException te) {
                    throw new ServerRtException("server.rt.err",te);
                }
            }
        }

        return r;
    }
    
    private void canPublish() {
        if (isPublished()) {
            throw new IllegalStateException(
                "Cannot publish this endpoint. Endpoint has been already published.");
        }
        if (stopped) {
            throw new IllegalStateException(
                "Cannot publish this endpoint. Endpoint has been already stopped.");
        }
    }

}
