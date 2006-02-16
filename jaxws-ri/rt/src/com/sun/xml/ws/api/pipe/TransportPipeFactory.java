package com.sun.xml.ws.api.pipe;

import com.sun.xml.ws.api.EndpointAddress;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.WSService;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.client.dispatch.StandalonePipeAssembler;
import com.sun.xml.ws.transport.http.client.HttpTransportPipe;
import com.sun.xml.ws.util.ServiceFinder;

import javax.xml.ws.WebServiceException;
import java.net.URL;
import java.util.logging.Logger;

/**
 * Factory for transport pipes that enables transport pluggability.
 *
 * <p>
 * At runtime, on the client side, JAX-WS (more specifically the default {@link PipelineAssembler}
 * of JAX-WS client runtime) relies on this factory to create a suitable transport {@link Pipe}
 * that can handle the given endpoint address.
 *
 * <p>
 * JAX-WS extensions that provide additional transport support can
 * extend this class and implement the {@link #doCreate} method.
 * They are expected to check the protocol of the endpoint address
 * (and possibly some other settings from bindings), and create
 * their transport pipe implementations accordingly.
 * (Endpoint address can be obtained from {@link WSDLPort#getAddress()}.)
 *
 * <p>
 * {@link TransportPipeFactory} look up follows the standard service
 * discovery mechanism, so you need
 * {@code META-INF/services/com.sun.xml.ws.api.pipe.TransportPipeFactory}.
 *
 * <h2>TODO</h2>
 * <p>
 * One of the JAX-WS operation mode is supposedly where it doesn't have no WSDL whatsoever.
 * How do we identify the endpoint in such case?
 *
 * <p>
 * {@link EndpointAddress} eagerly creates an {@link URL}, which is probably inconvenient
 * for a custom transport that wants to define custom scheme (like "jms", "xmpp", etc.)
 *
 * @author Kohsuke Kawaguchi
 * @see StandalonePipeAssembler
 */
public abstract class TransportPipeFactory {
    /**
     * Creates a transport {@link Pipe} for the given port, if this factory can do so,
     * or return null.
     *
     * @param wsdlModel
     *      The created transport pipe will be used to serve this port.
     *      Null if the service isn't associated with any port definition in WSDL,
     *      and otherwise non-null.
     *
     * @param service
     *      The transport pipe is created for this {@link WSService}.
     *      Always non-null.
     *
     * @param binding
     *      The binding of the new transport pipe to be created.
     *
     * @return
     *      null to indicate that this factory isn't capable of creating a transport
     *      for this port (which causes the caller to search for other {@link TransportPipeFactory}s
     *      that can. Or non-null.
     *
     * @throws WebServiceException
     *      if this factory is capable of creating a transport pipe but some fatal
     *      error prevented it from doing so. This exception will be propagated
     *      back to the user application, and no further {@link TransportPipeFactory}s
     *      are consulted.
     */
    public abstract Pipe doCreate(WSDLPort wsdlModel, WSService service, WSBinding binding);

    /**
     * Locates {@link PipelineAssemblerFactory}s and create
     * a suitable {@link PipelineAssembler}.
     *
     * @param classLoader
     *      used to locate {@code META-INF/servces} files.
     * @return
     *      Always non-null, since we fall back to our default {@link PipelineAssembler}.
     */
    public static Pipe create(ClassLoader classLoader, WSDLPort wsdlModel, WSService service, WSBinding binding) {
        for (TransportPipeFactory factory : ServiceFinder.find(TransportPipeFactory.class,classLoader)) {
            Pipe pipe = factory.doCreate(wsdlModel,service,binding);
            if(pipe!=null) {
                logger.fine(factory.getClass()+" successfully created "+pipe);
                return pipe;
            }
        }

        // default built-in trasnports
        EndpointAddress address = wsdlModel.getAddress();
        String protocol = address.getURL().getProtocol();
        if(protocol.equalsIgnoreCase("http") || protocol.equalsIgnoreCase("https"))
            return new HttpTransportPipe(binding);

        throw new WebServiceException("Unsupported endpoint address: "+address);    // TODO: i18n
    }

    private static final Logger logger = Logger.getLogger(TransportPipeFactory.class.getName());
}
