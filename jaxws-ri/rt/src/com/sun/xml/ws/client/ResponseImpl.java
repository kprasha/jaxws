package com.sun.xml.ws.client;

import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Response;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * {@link Response} implementation.
 *
 * @author Kohsuke Kawaguchi
 * @author Kathy Walsh
 */
public final class ResponseImpl<T> extends FutureTask<T> implements Response<T>, ResponseContextReceiver {

    /**
     * Optional {@link AsyncHandler} that gets invoked
     * at the completion of the task.
     */
    private final AsyncHandler<T> handler;
    private ResponseContext responseContext;

    /**
     *
     * @param callable
     *      This {@link Callable} is executed asynchronously.
     * @param handler
     *      Optional {@link AsyncHandler} to invoke at the end
     *      of the processing. Can be null.
     */
    public ResponseImpl(Callable<T> callable, AsyncHandler<T> handler) {
        super(callable);
        this.handler = handler;
    }

    @Override
    protected void done() {
        if (handler == null)
            return;

        try {
            if (!isCancelled())
                handler.handleResponse(this);
        } catch (Throwable e) {
            super.setException(e);
        }
    }

    public ResponseContext getContext() {
        return responseContext;
    }

    public void setResponseContext(ResponseContext rc) {
        responseContext = rc;
    }
}
