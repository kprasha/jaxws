/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License).  You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the license at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * 
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

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
