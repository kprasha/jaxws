package com.sun.xml.ws.client.port;

import com.sun.xml.ws.client.RequestContext;

import javax.xml.ws.Response;
import javax.xml.ws.WebServiceException;
import java.util.concurrent.Callable;

/**
 * {@link MethodHandler} that handles asynchronous invocations through {@link Response}.
 * @author Kohsuke Kawaguchi
 */
final class PollingMethodHandler extends MethodHandler {

    /**
     * The synchronous version of the method.
     */
    private final MethodHandler core;

    public PollingMethodHandler(PortInterfaceStub owner, MethodHandler core) {
        super(owner);
        this.core = core;
    }

    public Response<?> invoke(final Object proxy, final Object[] args, RequestContext rc) throws WebServiceException {
        // need to take a copy. required by the spec
        final RequestContext snapshot = rc.copy();

        final ResponseImpl[] r = new ResponseImpl[1];
        r[0] = new ResponseImpl<Object>(new Callable<Object>() {
            public Object call() throws Exception {
                return core.invoke(proxy,args,snapshot);
            }
        });

        owner.getExecutor().execute(r[0]);
        return r[0];
    }
}
