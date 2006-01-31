package com.sun.xml.ws.client.port;

import com.sun.xml.ws.client.RequestContext;
import com.sun.xml.ws.client.ResponseImpl;

import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Response;
import javax.xml.ws.WebServiceException;
import java.util.concurrent.Callable;

/**
 * Common part between {@link CallbackMethodHandler} and {@link PollingMethodHandler}.
 *
 * @author Kohsuke Kawaguchi
 */
abstract class AsyncMethodHandler extends MethodHandler {

    /**
     * The synchronous version of the method.
     */
    private final MethodHandler core;

    protected AsyncMethodHandler(PortInterfaceStub owner, MethodHandler core) {
        super(owner);
        this.core = core;
    }

    protected final Response<Object> doInvoke(final Object proxy, final Object[] args, RequestContext rc, AsyncHandler handler) {
        // need to take a copy. required by the spec
        final RequestContext snapshot = rc.copy();

        ResponseImpl<Object> ft = new ResponseImpl<Object>(new Callable<Object>() {
            public Object call() throws Exception {
                try {
                    return core.invoke(proxy,args,snapshot);
                } catch (Throwable throwable) {
                    throw new WebServiceException(throwable);
                }
            }
        },handler);

        owner.getExecutor().execute(ft);
        return ft;
    }

}
