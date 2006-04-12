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

    protected final Response<Object> doInvoke(Object proxy, Object[] args, AsyncHandler handler) {
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
