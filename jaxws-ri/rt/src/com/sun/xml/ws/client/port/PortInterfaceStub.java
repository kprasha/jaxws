package com.sun.xml.ws.client.port;

import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.model.wsdl.WSDLBoundOperation;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.binding.BindingImpl;
import com.sun.xml.ws.client.Stub;
import com.sun.xml.ws.model.JavaMethodImpl;
import com.sun.xml.ws.model.SOAPSEIModel;
import com.sun.xml.ws.pept.presentation.MEP;
import com.sun.xml.ws.util.Pool;

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
public final class PortInterfaceStub extends Stub implements InvocationHandler {
    public PortInterfaceStub(ServiceDelegate owner, BindingImpl binding, SOAPSEIModel seiModel, Pipe master) {
        super(master,binding);
        this.owner = owner;
        this.seiModel = seiModel;
        this.soapVersion = binding.getSOAPVersion();
        this.endpointAddress = seiModel.getPort().getAddress();

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
            if(jm.getMEP()== MEP.ASYNC_CALLBACK) {
                Method m = jm.getMethod();
                methodHandlers.put(m,new CallbackMethodHandler(this,
                    syncs.get(jm.getOperation()), m.getParameterTypes().length-1));
            }
            if(jm.getMEP()==MEP.ASYNC_POLL) {
                Method m = jm.getMethod();
                methodHandlers.put(m,new PollingMethodHandler(this,syncs.get(jm.getOperation())));
            }
        }
    }

    public final SOAPSEIModel seiModel;

    public final SOAPVersion soapVersion;

    /**
     * Cached value of {@code seiModel.getPort().getAddress()} for quicker access.
     */
    public final String endpointAddress;

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
            return handler.invoke(proxy, args, getRequestContext());
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

    public final Message doProcess(Message msg) {
        return super.process(msg);
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
