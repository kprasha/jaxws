package com.sun.xml.ws.client;

import com.sun.xml.ws.sandbox.message.Message;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.PipeCloner;
import com.sun.xml.ws.util.Pool;

import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;

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

    protected final Binding binding;

    private RequestContext requestContext = new RequestContext(this);
    private ResponseContext responseContext;

    /**
     * @param master
     *      The created stub will send messages to this pipe.
     * @param binding
     *      As a {@link BindingProvider}, this object will
     *      return this binding from {@link BindingProvider#getBinding()}.
     */
    protected Stub(Pipe master, Binding binding) {
        this.master = master;
        this.binding = binding;
        pipes.recycle(master);
    }

    /**
     * Passes a message to a pipe for processing.
     *
     * <p>
     * Unlike {@link Pipe#process(Message)},
     * this method is thread-safe and can be invoked from
     * multiple threads concurrently.
     */
    protected final Message process(Message msg) {
        Pipe pipe = pipes.take();
        try {
            return pipe.process(msg);
        } finally {
            // put it back to the pool
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
