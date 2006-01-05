package com.sun.xml.ws.client.port;

import com.sun.xml.ws.client.RequestContext;

import javax.xml.ws.AsyncHandler;
import javax.xml.ws.WebServiceException;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * {@link MethodHandler} that usee {@link AsyncHandler}.
 *
 * @author Kohsuke Kawaguchi
 */
final class CallbackMethodHandler extends MethodHandler {

    /**
     * The synchronous version of the method.
     */
    private final MethodHandler core;

    /**
     * Position of the argument that takes {@link AsyncHandler}.
     */
    private final int handlerPos;

    public CallbackMethodHandler(PortInterfaceStub owner, MethodHandler core, int handlerPos) {
        super(owner);
        this.core = core;
        this.handlerPos = handlerPos;
    }

    public Future<?> invoke(final Object proxy, final Object[] args, RequestContext rc) throws WebServiceException {
        // the spec requires the last argument
        final AsyncHandler handler = (AsyncHandler)args[handlerPos];

        // need to take a copy. required by the spec
        final RequestContext snapshot = rc.copy();

        final ResponseImpl[] r = new ResponseImpl[1];
        r[0] = new ResponseImpl<Object>(new Callable<Object>() {
            public Object call() throws Exception {
                Object t = core.invoke(proxy,args,snapshot);
                handler.handleResponse(r[0]);
                return t;
            }
        });

        owner.getExecutor().execute(r[0]);
        return r[0];
    }
}
