package com.sun.xml.ws.client;

import com.sun.istack.Nullable;
import com.sun.xml.ws.util.CompletedFuture;

import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Response;
import java.util.Map;
import java.util.concurrent.FutureTask;

/**
 * {@link Response} implementation. When Runnbale is executed, it just hands the
 * request to Fiber and returns. When the Fiber finishes the execution, it sets
 * response in the {@link FutureTask}
 *
 * <p>
 * {@link ResponseImpl} executes things synchronously and waits for the return
 * parameter.
 *
 * @author Jitendra Kotamraju
 */
public final class AsyncResponseImpl<T> extends FutureTask<T> implements Response<T>, ResponseContextReceiver {

    /**
     * Optional {@link AsyncHandler} that gets invoked
     * at the completion of the task.
     */
    private final AsyncHandler<T> handler;
    private ResponseContext responseContext;
    private final Runnable callable;

    /**
     *
     * @param runnable
     *      This {@link Runnable} is executed asynchronously.
     * @param handler
     *      Optional {@link AsyncHandler} to invoke at the end
     *      of the processing. Can be null.
     */
    public AsyncResponseImpl(Runnable runnable, @Nullable AsyncHandler<T> handler) {
        super(runnable, null);
        this.callable = runnable;
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
