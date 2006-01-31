package com.sun.xml.ws.client;

import javax.xml.ws.Response;
import javax.xml.ws.AsyncHandler;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Callable;
import java.util.Map;

/**
 * {@link Response} implementation.
 *
 * @author Kohsuke Kawaguchi
 * @author Kathy Walsh
 */
public final class ResponseImpl<T> extends FutureTask<T> implements Response<T> {

    /**
     * Optional {@link AsyncHandler} that gets invoked
     * at the completion of the task.
     */
    private final AsyncHandler<T> handler;

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

    public Map<String, Object> getContext() {
        // TODO
        throw new UnsupportedOperationException();
    }
}
