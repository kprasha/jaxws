package com.sun.xml.ws.api.pipe;

import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.WSService;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.model.RuntimeModel;
import com.sun.xml.ws.binding.BindingImpl;
import com.sun.xml.ws.client.dispatch.rearch.source.SourceDispatch;
import com.sun.xml.ws.client.dispatch.rearch.datasource.DataSourceDispatch;
import com.sun.xml.ws.client.dispatch.rearch.soapmsg.SOAPMessageDispatch;
import com.sun.xml.ws.client.port.PortInterfaceStub;
import com.sun.xml.ws.client.WSServiceDelegate;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Source;
import javax.activation.DataSource;
import java.lang.reflect.Proxy;

/**
 * Factory methods of various stubs.
 *
 * <p>
 * This class provides various methods to create "stub"s,
 * which are the component that turns a method invocation
 * into a {@link Message} and back into a return value.
 *
 * <p>
 * This class is meant to serve as the API from JAX-WS to
 * Tango, so that they don't have hard-code dependency on
 * our implementation classes.
 *
 * <a name="param"></a>
 * <h2>Common Parameters and Their Meanings</h2>
 *
 * <h3>Pipe next</h3>
 * <p>
 * Stubs turn a method invocation into a {@link Pipe#process(Message)} invocation,
 * and this pipe passed in as the <tt>next</tt> parameter will receive a {@link Message}
 * from newly created stub.
 *
 * <h3>BindingImpl binding</h3>
 * <p>
 * Stubs implement {@link BindingProvider}, and its {@link BindingProvider#getBinding()}
 * will return this <tt>binding</tt> object. Stubs often also use this information
 * to decide which SOAP version a {@link Message} should be created in.
 *
 * <h3>{@link WSService} service</h3>
 * <p>
 * This object represents a {@link Service} that owns the newly created stub.
 * For example, asynchronous method invocation will use {@link Service#getExecutor()}.
 *
 * @author Kohsuke Kawaguchi
 * @author Kathy Walsh
 */
public abstract class Stubs {
    private Stubs() {}   // no instanciation please

    /**
     * Creates a new {@link Dispatch} stub for {@link SOAPMessage}.
     *
     * This is short-cut of calling
     * <pre>
     * createDispatch(port,owner,binding,SOAPMessage.class,mode,next);
     * </pre>
     */
    public static Dispatch<SOAPMessage> createSAAJDispatch(QName portName, WSService owner, WSBinding binding, Service.Mode mode, Pipe next) {
        return new SOAPMessageDispatch(portName, SOAPMessage.class, mode, (WSServiceDelegate)owner, next, (BindingImpl)binding);
    }

    /**
     * Creates a new {@link Dispatch} stub for {@link DataSource}.
     *
     * This is short-cut of calling
     * <pre>
     * createDispatch(port,owner,binding,DataSource.class,mode,next);
     * </pre>
     */
    public static Dispatch<DataSource> createDataSourceDispatch(QName portName, WSService owner, WSBinding binding, Service.Mode mode, Pipe next) {
        return new DataSourceDispatch(portName, DataSource.class, mode, (WSServiceDelegate)owner, next, (BindingImpl)binding);
    }

    /**
     * Creates a new {@link Dispatch} stub for {@link Source}.
     *
     * This is short-cut of calling
     * <pre>
     * createDispatch(port,owner,binding,Source.class,mode,next);
     * </pre>
     */
    public static Dispatch<Source> createSourceDispatch(QName portName, WSService owner, WSBinding binding, Service.Mode mode, Pipe next) {
        return new SourceDispatch(portName, Source.class, mode, (WSServiceDelegate)owner, next, (BindingImpl)binding);
    }

    /**
     * Creates a new {@link Dispatch} stub that connects to the given pipe.
     *
     * @param portName
     *      see {@link Service#createDispatch(QName, Class, Service.Mode)}.
     * @param owner
     *      see <a href="#param">common parameters</a>
     * @param binding
     *      see <a href="#param">common parameters</a>
     * @param clazz
     *      Type of the {@link Dispatch} to be created.
     *      See {@link Service#createDispatch(QName, Class, Service.Mode)}.
     * @param mode
     *      The mode of the dispatch.
     *      See {@link Service#createDispatch(QName, Class, Service.Mode)}.
     * @param next
     *      see <a href="#param">common parameters</a>
     *
     * TODO: are these parameters making sense?
     */
    public static <T> Dispatch<T> createDispatch( QName portName,
                                           WSService owner,
                                           WSBinding binding,
                                           Class<T> clazz, Service.Mode mode, Pipe next ) {

        if (clazz == SOAPMessage.class) {
            return (Dispatch<T>) createSAAJDispatch(portName, owner, binding, mode, next);
        } else if (clazz == Source.class) {
            return (Dispatch<T>) createSourceDispatch(portName, owner, binding, mode, next);
        } else if (clazz == DataSource.class) {
            return (Dispatch<T>) createDataSourceDispatch(portName, owner, binding, mode, next);
        } else
            throw new WebServiceException("Unknown class type " + clazz.getName());
    }

    /**
     * Creates a new strongly-typed proxy object that implements a given port interface.
     *
     * @param service
     *      see <a href="#param">common parameters</a>
     * @param binding
     *      see <a href="#param">common parameters</a>
     * @param model
     *      This model shall represent a port interface.
     *      TODO: can model be constructed from portInterface and binding?
     *      Find out and update.
     * @param portInterface
     *      The port interface that has operations as Java methods.
     * @param next
     *      see <a href="#param">common parameters</a>
     */
    public <T> T createPortProxy( WSService service, WSBinding binding, RuntimeModel model,
                                  Class<T> portInterface, Pipe next ) {

        PortInterfaceStub ps = new PortInterfaceStub(service,(BindingImpl)binding,portInterface,model,next);
        return portInterface.cast(
            Proxy.newProxyInstance( portInterface.getClassLoader(),
                new Class[]{portInterface,BindingProvider.class}, ps ));
    }
}
