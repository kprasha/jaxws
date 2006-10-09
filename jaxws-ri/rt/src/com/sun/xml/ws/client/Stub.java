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

package com.sun.xml.ws.client;

import com.sun.istack.NotNull;
import com.sun.istack.Nullable;
import com.sun.xml.ws.Closeable;
import com.sun.xml.ws.addressing.EndpointReferenceUtil;
import com.sun.xml.ws.api.EndpointAddress;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.addressing.WSEndpointReference;
import com.sun.xml.ws.api.message.HeaderList;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.pipe.Engine;
import com.sun.xml.ws.api.pipe.Fiber;
import com.sun.xml.ws.api.pipe.Tube;
import com.sun.xml.ws.binding.BindingImpl;
import com.sun.xml.ws.util.Pool;
import com.sun.xml.ws.util.Pool.PipePool;
import com.sun.xml.ws.util.RuntimeVersion;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.EndpointReference;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.wsaddressing.W3CEndpointReference;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Base class for stubs, which accept method invocations from
 * client applications and pass the message to a {@link Tube}
 * for processing.
 *
 * <p>
 * This class implements the management of pipe instances,
 * and most of the {@link BindingProvider} methods.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class Stub implements BindingProvider, ResponseContextReceiver, Closeable {

    /**
     * Reuse pipelines as it's expensive to create.
     * <p>
     * Set to null when {@link #close() closed}.
     */
    private Pool<Tube> tubes;

    private final Engine engine;

    /**
     * The {@link WSServiceDelegate} object that owns us.
     */
    protected final WSServiceDelegate owner;

    /**
     * Non-null if this stub is configured to talk to an EPR.
     * <p>
     * When this field is non-null, its reference parameters are sent as out-bound headers.
     * This field can be null even when addressing is enabled, but if the addressing is
     * not enabled, this field must be null.
     */
    protected @Nullable WSEndpointReference endpointReference;

    protected final BindingImpl binding;

    public final RequestContext requestContext = new RequestContext();

    /**
     * {@link ResponseContext} from the last synchronous operation.
     */
    private ResponseContext responseContext;
    @Nullable protected final WSDLPort wsdlPort;

    /**
     * @param master                 The created stub will send messages to this pipe.
     * @param binding                As a {@link BindingProvider}, this object will
     *                               return this binding from {@link BindingProvider#getBinding()}.
     * @param defaultEndPointAddress The destination of the message. The actual destination
     *                               could be overridden by {@link RequestContext}.
     */
    protected Stub(WSServiceDelegate owner, Tube master, BindingImpl binding, WSDLPort wsdlPort, EndpointAddress defaultEndPointAddress) {
        this.owner = owner;
        this.tubes = new PipePool(master);
        this.wsdlPort = wsdlPort;
        this.binding = binding;
        this.requestContext.setEndpointAddress(defaultEndPointAddress);
        engine = new Engine();
    }

    /**
     * Gets the port name that this stub is configured to talk to.
     * <p>
     * When {@link #wsdlPort} is non-null, the port name is always
     * the same as {@link WSDLPort#getName()}, but this method
     * returns a port name even if no WSDL is available for this stub.
     */
    protected abstract @NotNull QName getPortName();

    /**
     * Gets the service name that this stub is configured to talk to.
     * <p>
     * When {@link #wsdlPort} is non-null, the service name is always
     * the same as the one that's inferred from {@link WSDLPort#getOwner()},
     * but this method returns a port name even if no WSDL is available for
     * this stub.
     */
    protected final @NotNull QName getServiceName() {
        return owner.getServiceName();
    }

    /**
     * Gets the {@link Executor} to be used for asynchronous method invocations.
     * <p/>
     * <p/>
     * Note that the value this method returns may different from invocations
     * to invocations. The caller must not cache.
     *
     * @return always non-null.
     */
    protected final Executor getExecutor() {
        return owner.getExecutor();
    }
    
    /**
     * Passes a message to a pipe for processing.
     * <p>
     * Unlike {@link Tube} instances,
     * this method is thread-safe and can be invoked from
     * multiple threads concurrently.
     *
     * @param packet         The message to be sent to the server
     * @param requestContext The {@link RequestContext} when this invocation is originally scheduled.
     *                       This must be the same object as {@link #requestContext} for synchronous
     *                       invocations, but for asynchronous invocations, it needs to be a snapshot
     *                       captured at the point of invocation, to correctly satisfy the spec requirement.
     * @param receiver       Receives the {@link ResponseContext}. Since the spec requires
     *                       that the asynchronous invocations must not update response context,
     *                       depending on the mode of invocation they have to go to different places.
     *                       So we take a setter that abstracts that away.
     */
    protected final Packet process(Packet packet, RequestContext requestContext, ResponseContextReceiver receiver) {
        {// fill in Packet
            packet.proxy = this;
            packet.handlerConfig = binding.getHandlerConfig();
            requestContext.fill(packet);
            if (binding.isAddressingEnabled()) {
                HeaderList headerList = packet.getMessage().getHeaders();
                headerList.fillRequestAddressingHeaders(wsdlPort, binding, packet);
                if(endpointReference!=null)
                    endpointReference.addReferenceParameters(headerList);
            }
        }

        Packet reply;

        Pool<Tube> pool = tubes;
        if (pool == null)
            throw new WebServiceException("close method has already been invoked"); // TODO: i18n

        Fiber fiber = engine.createFiber();
        // then send it away!
        Tube tube = pool.take();
        try {
            reply = fiber.runSync(tube, packet);
        } finally {
            pool.recycle(tube);
        }

        // not that Packet can still be updated after
        // ResponseContext is created.
        receiver.setResponseContext(new ResponseContext(reply));

        return reply;
    }

    public void close() {
        if (tubes != null) {
            // multi-thread safety of 'close' needs to be considered more carefully.
            // some calls might be pending while this method is invoked. Should we
            // block until they are complete, or should we abort them (but how?)
            Tube p = tubes.take();
            tubes = null;
            p.preDestroy();
        }
    }

    public final WSBinding getBinding() {
        return binding;
    }

    public final Map<String, Object> getRequestContext() {
        return requestContext.getMapView();
    }

    public final ResponseContext getResponseContext() {
        return responseContext;
    }

    public void setResponseContext(ResponseContext rc) {
        this.responseContext = rc;
    }

    public String toString() {
        return RuntimeVersion.VERSION + ": Stub for " + getRequestContext().get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY);
    }

    public final W3CEndpointReference getEndpointReference(Element...referenceParameters) {
        return getEndpointReference(W3CEndpointReference.class, referenceParameters);
    }

    public final <T extends EndpointReference>
    T getEndpointReference(Class<T> clazz, Element...referenceParameters) {
        if (endpointReference != null) {
            return endpointReference.toSpec(clazz);
        }

        QName portTypeName = null;

        if(wsdlPort!=null)
            portTypeName = wsdlPort.getBinding().getPortTypeName();

        return clazz.cast(EndpointReferenceUtil.getEndpointReference(clazz,
                requestContext.getEndpointAddress().toString(),
                getServiceName(),
                getPortName().getLocalPart(),
                portTypeName, wsdlPort!=null));
    }
}
