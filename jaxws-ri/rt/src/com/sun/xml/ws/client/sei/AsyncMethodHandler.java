package com.sun.xml.ws.client.sei;

import com.sun.xml.ws.client.RequestContext;
import com.sun.xml.ws.client.ResponseImpl;
import com.sun.xml.ws.client.ResponseContextReceiver;

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
    private final SyncMethodHandler core;

    protected AsyncMethodHandler(SEIStub owner, SyncMethodHandler core) {
        super(owner);
        this.core = core;
    }

    protected final Response<Object> doInvoke(final Object proxy, final Object[] args, AsyncHandler handler) {
        AsyncMethodHandler.Invoker invoker = new Invoker(proxy, args);
        ResponseImpl<Object> ft = new ResponseImpl<Object>(invoker,handler);
        invoker.setReceiver(ft);
        
        owner.getExecutor().execute(ft);
        return ft;
    }

    private class Invoker implements Callable<Object> {
        private final Object proxy;
        private final Object[] args;
        // need to take a copy. required by the spec
        private final RequestContext snapshot = owner.requestContext.copy();
        /**
         * Because of the object instantiation order,
         * we can't take this as a constructor parameter.
         */
        private ResponseContextReceiver receiver;

        public Invoker(Object proxy, Object[] args) {
            this.proxy = proxy;
            this.args = args;
        }

        public Object call() throws Exception {
            assert receiver!=null;
            try {
                return core.invoke(proxy,args,snapshot,receiver);
            } catch (Throwable t) {
                throw new WebServiceException(t);
            }
        }

        void setReceiver(ResponseContextReceiver receiver) {
            this.receiver = receiver;
        }
    }
}
