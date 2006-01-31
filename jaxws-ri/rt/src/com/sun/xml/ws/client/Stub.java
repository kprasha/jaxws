package com.sun.xml.ws.client;

import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.MessageProperties;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.PipeCloner;
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
public abstract class Stub implements BindingProvider {

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
    private final String defaultEndPointAddress;

    private RequestContext requestContext = new RequestContext(this);
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
    protected Stub(Pipe master, BindingImpl binding, String defaultEndPointAddress) {
        this.master = master;
        this.binding = binding;
        this.defaultEndPointAddress = defaultEndPointAddress;
        pipes.recycle(master);
    }

    /**
     * Passes a message to a pipe for processing.
     *
     * <p>
     * Unlike {@link Pipe#process(Message)},
     * this method is thread-safe and can be invoked from
     * multiple threads concurrently.
     *
     * @param msg
     *      The message to be sent to the server
     * @param requestContext
     *      The {@link RequestContext} when this invocation is originally scheduled.
     *      This should be the same object as {@link #requestContext} for synchronous
     *      invocations, but for asynchronous invocations, it needs to be a snapshot
     *      captured at the point of invocation, to correctly satisfy the spec requirement.
     */
    protected final Message process(Message msg, RequestContext requestContext) {
        {// fill in MessageProperties
            MessageProperties props = msg.getProperties();

            props.proxy = this;
            props.endpointAddress = defaultEndPointAddress;

            if(!requestContext.isEmpty()) {
                for (Entry<String, Object> entry : requestContext.entrySet()) {
                    String key = entry.getKey();
                    if(props.supports(key))
                        props.put(key,entry.getValue());
                    else
                        props.invocationProperties.put(key,entry.getValue());
                }
            }
        }

        // then send it away!
        Pipe pipe = pipes.take();
        try {
            return pipe.process(msg);
        } finally {
            pipes.recycle(pipe);
        }
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
}
