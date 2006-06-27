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

import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.model.wsdl.WSDLBoundOperation;
import com.sun.xml.ws.api.model.MEP;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.binding.BindingImpl;
import com.sun.xml.ws.client.RequestContext;
import com.sun.xml.ws.client.Stub;
import com.sun.xml.ws.client.ResponseContextReceiver;
import com.sun.xml.ws.model.JavaMethodImpl;
import com.sun.xml.ws.model.SOAPSEIModel;

import javax.xml.ws.WebServiceException;
import javax.xml.ws.spi.ServiceDelegate;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * {@link Stub} that handles method invocations
 * through a strongly-typed endpoint interface.
 *
 * @author Kohsuke Kawaguchi
 */
public final class SEIStub extends Stub implements InvocationHandler {
    public SEIStub(ServiceDelegate owner, BindingImpl binding, SOAPSEIModel seiModel, Pipe master) {
        super(master,binding,seiModel.getPort().getAddress());
        this.owner = owner;
        this.seiModel = seiModel;
        this.soapVersion = binding.getSOAPVersion();

        Map<WSDLBoundOperation,SyncMethodHandler> syncs = new HashMap<WSDLBoundOperation, SyncMethodHandler>();

        // fill in methodHandlers.
        // first fill in sychronized versions
        for( JavaMethodImpl m : seiModel.getJavaMethods() ) {
            if(!m.getMEP().isAsync) {
                SyncMethodHandler handler = new SyncMethodHandler(this, m);
                syncs.put(m.getOperation(),handler);
                methodHandlers.put(m.getMethod(),handler);
            }
        }

        for( JavaMethodImpl jm : seiModel.getJavaMethods() ) {
            SyncMethodHandler sync = syncs.get(jm.getOperation());
            if(jm.getMEP()== MEP.ASYNC_CALLBACK) {
                Method m = jm.getMethod();
                CallbackMethodHandler handler = new CallbackMethodHandler(
                        this, jm, sync, m.getParameterTypes().length-1);
                methodHandlers.put(m, handler);
            }
            if(jm.getMEP()== MEP.ASYNC_POLL) {
                Method m = jm.getMethod();
                PollingMethodHandler handler = new PollingMethodHandler(this, jm, sync);
                methodHandlers.put(m, handler);
            }
        }
    }

    public final SOAPSEIModel seiModel;

    public final SOAPVersion soapVersion;

    /**
     * The {@link ServiceDelegate} object that owns us.
     */
    public final ServiceDelegate owner;

    /**
     * For each method on the port interface we have
     * a {@link MethodHandler} that processes it.
     */
    private final Map<Method,MethodHandler> methodHandlers = new HashMap<Method,MethodHandler>();

    public Object invoke(Object proxy, Method method, Object[] args) throws WebServiceException, Throwable {
        MethodHandler handler = methodHandlers.get(method);
        if(handler!=null) {
            return handler.invoke(proxy,args);
        } else {
            // we handle the other method invocations by ourselves
            try {
                return method.invoke(this,args);
            } catch (IllegalAccessException e) {
                // impossible
                throw new AssertionError(e);
            } catch (IllegalArgumentException e) {
                throw new AssertionError(e);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }
    }

    public final Packet doProcess(Packet request, RequestContext rc, ResponseContextReceiver receiver) {
        return super.process(request,rc,receiver);
    }

    /**
     * Gets the {@link Executor} to be used for asynchronous method invocations.
     *
     * <p>
     * Note that the value this method returns may different from invocations
     * to invocations. The caller must not cache.
     *
     * @return
     *      always non-null.
     */
    protected final Executor getExecutor() {
        return owner.getExecutor();
    }
}
