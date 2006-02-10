package com.sun.xml.ws.client;

import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.PipeCloner;
import com.sun.xml.ws.api.EndpointAddress;
import com.sun.xml.ws.binding.BindingImpl;
import com.sun.xml.ws.util.Pool;

import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;
import java.util.Map.Entry;

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
public abstract class Stub implements BindingProvider, ResponseContextReceiver {

    /**
     * Reuse pipelines as it's expensive to create.
     */
    protected final Pool<Pipe> pipes = new Pool<Pipe>() {
        protected Pipe create() {
            return PipeCloner.clone(master);
        }
    };

    /**
     * Master {@link Pipe} instance from which
     * copies are created.
     * <p>
     * We'll always keep at least one {@link Pipe}
     * so that we can copy new ones. Note that
     * this pipe is also in {@link #pipes} and therefore
     * can be used to process messages like any other pipes.
     */
    private final Pipe master;

    protected final BindingImpl binding;

    /**
     * The address to which the message is sent to,
     * (unless it's overriden in {@link RequestContext}.
     */
    private final EndpointAddress defaultEndPointAddress;

    private RequestContext requestContext = new RequestContext(this);

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
        this.master = master;
        this.binding = binding;
        this.defaultEndPointAddress = defaultEndPointAddress;
        pipes.recycle(master);
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
            
            if(!requestContext.isEmpty()) {
                for (Entry<String, Object> entry : requestContext.entrySet()) {
                    String key = entry.getKey();
                    if(packet.supports(key))
                        packet.put(key,entry.getValue());
                    else
                        packet.invocationProperties.put(key,entry.getValue());
                }
            }
        }

        Packet reply;

        // then send it away!
        Pipe pipe = pipes.take();
        try {
            reply = pipe.process(packet);
        } finally {
            pipes.recycle(pipe);
        }

        // not that Packet can still be updated after
        // ResponseContext is created.
        receiver.setResponseContext(new ResponseContext(reply));

        return reply;
    }

    public final Binding getBinding() {
        return binding;
    }

    public final RequestContext getRequestContext() {
        return requestContext;
    }

    public final ResponseContext getResponseContext() {
        return responseContext;
    }

    public void setResponseContext(ResponseContext rc) {
        this.responseContext = rc;
    }
}
