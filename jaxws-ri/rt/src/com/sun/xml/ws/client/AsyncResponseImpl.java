package com.sun.xml.ws.client;

import com.sun.xml.ws.util.CompletedFuture;
import com.sun.xml.ws.api.message.Packet;
import com.sun.istack.NotNull;

import javax.xml.ws.Response;
import javax.xml.ws.AsyncHandler;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Callable;
import java.util.Map;

/**
 * {@link javax.xml.ws.Response} implementation.
 *
 * @author Jitendra Kotamraju
 */
public final class AsyncResponseImpl<T> extends FutureTask<T> implements Response<T>, ResponseContextReceiver {

    /**
     * Optional {@link javax.xml.ws.AsyncHandler} that gets invoked
     * at the completion of the task.
     */
    private final AsyncHandler<T> handler;
    private ResponseContext responseContext;
    private final Runnable callable;

    /**
     *
     * @param callable
     *      This {@link Runnable} is executed asynchronously.
     * @param handler
     *      Optional {@link javax.xml.ws.AsyncHandler} to invoke at the end
     *      of the processing. Can be null.
     */
    public AsyncResponseImpl(Runnable callable, AsyncHandler<T> handler) {
        super(callable, null);
        this.callable = callable;
        this.handler = handler;
    }

    @Override
    public void run() {
        // override so that AsyncInvoker calls set()
        // when Fiber calls the callback
        callable.run();
    }


    public ResponseContext getContext() {
        return responseContext;
    }

    public void setResponseContext(ResponseContext rc) {
        responseContext = rc;
    }

    public void set(final T v, final Throwable t) {
        // call the handler before we mark the future as 'done'
        if (handler!=null) {
            try {
                /**
                 * {@link Response} object passed into the callback.
                 * We need a separate {@link java.util.concurrent.Future} because we don't want {@link ResponseImpl}
                 * to be marked as 'done' before the callback finishes execution.
                 * (That would provide implicit synchronization between the application code
                 * in the main thread and the callback code, and is compatible with the JAX-RI 2.0 FCS.
                 */
                class CallbackFuture<T> extends CompletedFuture<T> implements Response<T> {
                    public CallbackFuture(T v, Throwable t) {
                        super(v, t);
                    }

                    public Map<String, Object> getContext() {
                        return AsyncResponseImpl.this.getContext();
                    }
                }
                handler.handleResponse(new CallbackFuture<T>(v, t));
            } catch (Throwable e) {
                super.setException(e);
                return;
            }
        }
        if (t != null) {
            super.setException(t);
        } else {
            super.set(v);
        }
    }
}
