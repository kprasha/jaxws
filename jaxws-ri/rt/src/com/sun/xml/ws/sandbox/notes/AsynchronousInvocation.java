package com.sun.xml.ws.sandbox.notes;

import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Response;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 * Note about asynchronous invocation.
 *
 * <p>
 * Asynchronous operation handling should be the very first thing
 * you do on the proxy class or a {@link Dispatch} object, since
 * they can be done as a wrapper around a synchronous method invocation
 * like below.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class AsynchronousInvocation<T> implements Dispatch<T> {
    private final Executor exec = Executors.newCachedThreadPool(); /* or whatever */

    public Response<T> invokeAsync(final T param) {
        ResponseImpl<T> ft = new ResponseImpl<T>(new Callable() {
            public T call() throws Exception {
                return invoke(param);
            }
        });

        exec.execute(ft);
        return ft;
    }

    public Future<?> invokeAsync(final T param, final AsyncHandler<T> asyncHandler) {
        final ResponseImpl<T>[] r = new ResponseImpl[1];
        r[0] = new ResponseImpl<T>(new Callable() {
            public T call() throws Exception {
                T t = invoke(param);
                asyncHandler.handleResponse(r[0]);
                return t;
            }
        });

        exec.execute(r[0]);
        return r[0];
    }

    private static class ResponseImpl<T> extends FutureTask<T> implements Response<T> {
        protected ResponseImpl(Callable<T> callable) {
            super(callable);
        }

        public Map<String, Object> getContext() {
            // TODO
            throw new UnsupportedOperationException();
        }
    }
}
