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

package com.sun.xml.ws.api.pipe;

import com.sun.xml.ws.api.EndpointAddress;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.WSService;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.client.dispatch.StandalonePipeAssembler;
import com.sun.xml.ws.transport.http.client.HttpTransportPipe;
import com.sun.xml.ws.util.ServiceFinder;
import com.sun.istack.NotNull;
import com.sun.istack.Nullable;

import javax.xml.ws.WebServiceException;
import java.util.logging.Logger;

/**
 * Factory for transport pipes that enables transport pluggability.
 *
 * <p>
 * At runtime, on the client side, JAX-WS (more specifically the default {@link PipelineAssembler}
 * of JAX-WS client runtime) relies on this factory to create a suitable transport {@link Pipe}
 * that can handle the given {@link EndpointAddress endpoint address}.
 *
 * <p>
 * JAX-WS extensions that provide additional transport support can
 * extend this class and implement the {@link #doCreate} method.
 * They are expected to check the scheme of the endpoint address
 * (and possibly some other settings from bindings), and create
 * their transport pipe implementations accordingly.
 * For example,
 *
 * <pre>
 * class MyTransportPipeFactoryImpl {
 *   Pipe doCreate(...) {
 *     String scheme = address.getURI().getScheme();
 *     if(scheme.equals("foo"))
 *       return new MyTransport(...);
 *     else
 *       return null;
 *   }
 * }
 * </pre>
 *
 * <p>
 * {@link TransportPipeFactory} look-up follows the standard service
 * discovery mechanism, so you need
 * {@code META-INF/services/com.sun.xml.ws.api.pipe.TransportPipeFactory}.
 *
 *
 *
 * <h2>TODO</h2>
 * <p>
 * One of the JAX-WS operation mode is supposedly where it doesn't have no WSDL whatsoever.
 * How do we identify the endpoint in such case?
 *
 * @author Kohsuke Kawaguchi
 * @see StandalonePipeAssembler
 */
public abstract class TransportPipeFactory {
    /**
     * Creates a transport {@link Pipe} for the given port, if this factory can do so,
     * or return null.
     *
     * @param address
     *      The endpoint address. Always non-null. This parameter is taken separately
     *      from {@link WSDLPort} (even though there's {@link WSDLPort#getAddress()})
     *      because sometimes WSDL is not available.
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
    public abstract Pipe doCreate(@NotNull EndpointAddress address, @Nullable WSDLPort wsdlModel, @NotNull WSService service, @NotNull WSBinding binding);

    /**
     * Locates {@link PipelineAssemblerFactory}s and create
     * a suitable {@link PipelineAssembler}.
     *
     * @param classLoader
     *      used to locate {@code META-INF/servces} files.
     * @return
     *      Always non-null, since we fall back to our default {@link PipelineAssembler}.
     */
    public static Pipe create(@Nullable ClassLoader classLoader, @NotNull EndpointAddress address, @Nullable WSDLPort wsdlModel, @NotNull WSService service, @NotNull WSBinding binding) {
        for (TransportPipeFactory factory : ServiceFinder.find(TransportPipeFactory.class,classLoader)) {
            Pipe pipe = factory.doCreate(address,wsdlModel,service,binding);
            if(pipe!=null) {
                logger.fine(factory.getClass()+" successfully created "+pipe);
                return pipe;
            }
        }

        // default built-in trasnports
        String scheme = address.getURI().getScheme();
        if (scheme != null) {
            if(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
                return new HttpTransportPipe(binding);
        }

        throw new WebServiceException("Unsupported endpoint address: "+address);    // TODO: i18n
    }

    private static final Logger logger = Logger.getLogger(TransportPipeFactory.class.getName());
}
