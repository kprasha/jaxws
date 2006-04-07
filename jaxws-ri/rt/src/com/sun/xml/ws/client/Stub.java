package com.sun.xml.ws.client;

import com.sun.xml.ws.Closeable;
import com.sun.xml.ws.api.EndpointAddress;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.binding.BindingImpl;
import com.sun.xml.ws.util.Pool;
import com.sun.xml.ws.util.RuntimeVersion;
import com.sun.xml.ws.util.Pool.PipePool;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceException;
import java.util.Map;

/**
 * Base class for stubs, which accept method invocations from
 * client applications and pass the message to a {@link Pipe}
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
     *
     * Set to null when {@link #close() closed}.
     */
    private Pool<Pipe> pipes;

    protected final BindingImpl binding;

    /**
     * The address to which the message is sent to,
     * (unless it's overriden in {@link RequestContext}.
     */
    private final EndpointAddress defaultEndPointAddress;

    public final RequestContext requestContext = new RequestContext();

    /**
     * {@link ResponseContext} from the last synchronous operation.
     */
    private ResponseContext responseContext;

    /**
     * @param master
     *      The created stub will send messages to this pipe.
     * @param binding
     *      As a {@link BindingProvider}, this object will
     *      return this binding from {@link BindingProvider#getBinding()}.
     * @param defaultEndPointAddress
     *      The destination of the message. The actual destination
     *      could be overridden by {@link RequestContext}.
     */
    protected Stub(Pipe master, BindingImpl binding, EndpointAddress defaultEndPointAddress) {
        this.pipes = new PipePool(master);
        this.binding = binding;
        this.defaultEndPointAddress = defaultEndPointAddress;
    }

    /**
     * Passes a message to a pipe for processing.
     *
     * <p>
     * Unlike {@link Pipe#process(Packet)},
     * this method is thread-safe and can be invoked from
     * multiple threads concurrently.
     *
     * @param packet
     *      The message to be sent to the server
     * @param requestContext
     *      The {@link RequestContext} when this invocation is originally scheduled.
     *      This must be the same object as {@link #requestContext} for synchronous
     *      invocations, but for asynchronous invocations, it needs to be a snapshot
     *      captured at the point of invocation, to correctly satisfy the spec requirement.
     * @param receiver
     *      Receives the {@link ResponseContext}. Since the spec requires
     *      that the asynchronous invocations must not update response context,
     *      depending on the mode of invocation they have to go to different places.
     *      So we take a setter that abstracts that away. 
     */
    protected final Packet process(Packet packet, RequestContext requestContext, ResponseContextReceiver receiver) {
        {// fill in Packet
            packet.proxy = this;
            packet.endpointAddress = defaultEndPointAddress;

            requestContext.fill(packet);
        }

        Packet reply;

        Pool<Pipe> pool = pipes;
        if(pool==null)
            throw new WebServiceException("close method has already been invoked"); // TODO: i18n

        // then send it away!
        Pipe pipe = pool.take();
        try {
            reply = pipe.process(packet);
        } finally {
            pool.recycle(pipe);
        }

        // not that Packet can still be updated after
        // ResponseContext is created.
        receiver.setResponseContext(new ResponseContext(reply));

        return reply;
    }

    public void close() {
        if(pipes!=null) {
            // multi-thread safety of 'close' needs to be considered more carefully.
            // some calls might be pending while this method is invoked. Should we
            // block until they are complete, or should we abort them (but how?)
            Pipe p = pipes.take();
            pipes = null;
            p.preDestroy();
        }
    }

    public final WSBinding getBinding() {
        return binding;
    }

    public final Map<String,Object> getRequestContext() {
        return requestContext.getMapView();
    }

    public final ResponseContext getResponseContext() {
        return responseContext;
    }

    public void setResponseContext(ResponseContext rc) {
        this.responseContext = rc;
    }

    public String toString() {
        return RuntimeVersion.VERSION+": Stub for "+getRequestContext().get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY);
    }
}
