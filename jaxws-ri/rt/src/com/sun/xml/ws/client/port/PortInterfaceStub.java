package com.sun.xml.ws.client.port;

import com.sun.xml.ws.binding.BindingImpl;
import com.sun.xml.ws.client.Stub;
import com.sun.xml.ws.encoding.soap.SOAPVersion;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.model.JavaMethod;
import com.sun.xml.ws.api.model.RuntimeModel;
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
    public PortInterfaceStub(ServiceDelegate owner, BindingImpl binding, Class portInterface, RuntimeModel model, Pipe master ) {
        super(master,binding);
        this.owner = owner;
        this.model = model;
        this.portInterface = portInterface;
        this.soapVersion = SOAPVersion.fromBinding(binding.getBindingId());

        this.marshallers = new Pool.Marshaller(model.getJAXBContext());
        this.bridgeContexts = new Pool.BridgeContext(model.getJAXBContext());

        // fill in methodHandlers.
        // first fill in sychronized versions
        for( JavaMethod m : model.getJavaMethods() ) {
            if(!m.getMEP().isAsync) {
                methodHandlers.put(m.getMethod(),new SyncMethodHandler(this,m));
            }
        }

        // TODO: fill in asynchronous versions by using the synchronized versions
    }

    public final RuntimeModel model;

    public final SOAPVersion soapVersion;

    /**
     * Port interface that this proxy implements.
     */
    private final Class portInterface;

    /**
     * The {@link ServiceDelegate} object that owns us.
     */
    public final ServiceDelegate owner;

    /**
     * For each method on the {@link #portInterface} we have
     * a {@link MethodHandler} that processes it.
     */
    private final Map<Method,MethodHandler> methodHandlers = new HashMap<Method,MethodHandler>();

    /**
     * JAXB marshaller pool.
     *
     * TODO: this pool can be shared across {@link Stub}s.
     */
    public final Pool.Marshaller marshallers;

    // TODO: ditto
    public final Pool.BridgeContext bridgeContexts;

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
