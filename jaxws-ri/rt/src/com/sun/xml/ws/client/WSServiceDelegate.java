/*
 * Copyright (c) 2005 Sun Microsystems Corporation. All Rights Reserved.
 */
package com.sun.xml.ws.client;

import com.sun.org.apache.xml.internal.resolver.tools.CatalogResolver;
import com.sun.xml.ws.api.WSService;
import com.sun.xml.ws.api.model.RuntimeModel;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.PipelineAssembler;
import com.sun.xml.ws.binding.BindingImpl;
import com.sun.xml.ws.binding.http.HTTPBindingImpl;
import com.sun.xml.ws.binding.soap.SOAPBindingImpl;
import com.sun.xml.ws.client.dispatch.DispatchBase;
import com.sun.xml.ws.client.dispatch.rearch.DispatchFactory;
import com.sun.xml.ws.client.dispatch.rearch.StandalonePipeAssembler;
import com.sun.xml.ws.client.dispatch.rearch.jaxb.JAXBDispatch;
import com.sun.xml.ws.handler.PortInfoImpl;
import com.sun.xml.ws.model.AbstractRuntimeModelImpl;
import com.sun.xml.ws.util.xml.XmlUtil;
import com.sun.xml.ws.wsdl.WSDLContext;
import org.xml.sax.EntityResolver;

import javax.xml.bind.JAXBContext;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.HandlerResolver;
import javax.xml.ws.handler.PortInfo;
import javax.xml.ws.http.HTTPBinding;
import javax.xml.ws.soap.SOAPBinding;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * <code>Service</code> objects provide the client view of a Web service.
 * <p><code>Service</code> acts as a factory of the following:
 * <ul>
 * <li>Proxies for a target service endpoint.
 * <li>Instances of <code>javax.xml.ws.Dispatch</code> for
 * dynamic message-oriented invocation of a remote
 * operation.
 * </li>
 * <p/>
 * <p>The ports available on a service can be enumerated using the
 * <code>getPorts</code> method. Alternatively, you can pass a
 * service endpoint interface to the unary <code>getPort</code> method
 * and let the runtime select a compatible port.
 * <p/>
 * <p>Handler chains for all the objects created by a <code>Service</code>
 * can be set by means of the provided <code>HandlerRegistry</code>.
 * <p/>
 * <p>An <code>Executor</code> may be set on the service in order
 * to gain better control over the threads used to dispatch asynchronous
 * callbacks. For instance, thread pooling with certain parameters
 * can be enabled by creating a <code>ThreadPoolExecutor</code> and
 * registering it with the service.
 *
 * @author WS Development Team
 * @see java.util.concurrent.Executor
 * @since JAX-WS 2.0
 */
public class WSServiceDelegate extends WSService {

    protected static final String GET = "get";


    protected HashSet<QName> ports;

    protected HashMap<QName, PortInfoBase> dispatchPorts;
    protected HandlerResolver handlerResolver;

    protected Object serviceProxy;
    protected URL wsdlLocation;
    protected ServiceContext serviceContext;
    protected Executor executor;
    private HashSet<Object> seiProxies;
    private Pipe masterPipe;

    //to get this going just use this--?do we need factory for different assembler types?
    private static PipelineAssembler assembler = new StandalonePipeAssembler();

    /**
     * {@link CatalogResolver} to check META-INF/jax-ws-catalog.xml.
     * Lazily created.
     */
    private EntityResolver entityResolver;


    public WSServiceDelegate(ServiceContext scontext) {
        serviceContext = scontext;
        this.dispatchPorts = new HashMap();
        seiProxies = new HashSet();
        if (serviceContext.getHandlerResolver() != null) {
            handlerResolver = serviceContext.getHandlerResolver();
        }
        assembler = new StandalonePipeAssembler();

        masterPipe = ((StandalonePipeAssembler) assembler).create(this);

    }

    public WSServiceDelegate(URL wsdlDocumentLocation, QName serviceName, Class serviceClass) {
        this.dispatchPorts = new HashMap();
        //this.ports = new HashSet();
        seiProxies = new HashSet();

        if (wsdlDocumentLocation != null) {
            serviceContext = ServiceContextBuilder.build(
                wsdlDocumentLocation, serviceClass, XmlUtil.createDefaultCatalogResolver());

        } else {
            serviceContext = new ServiceContext(XmlUtil.createDefaultCatalogResolver());
            serviceContext.setServiceName(serviceName);
        }
        if (serviceContext.getHandlerResolver() != null) {
            handlerResolver = serviceContext.getHandlerResolver();
        }
        if (ports == null)
            populatePorts();
        masterPipe = ((StandalonePipeAssembler) assembler).create(this);
    }

    private void processServiceContext(QName portName, Class portInterface) throws WebServiceException {
        ServiceContextBuilder.completeServiceContext(serviceContext, portInterface);
    }

    public URL getWSDLLocation() {
        if (wsdlLocation == null)
            setWSDLLocation(getWsdlLocation());
        return wsdlLocation;
    }

    public void setWSDLLocation(URL location) {
        wsdlLocation = location;
    }

    public Executor getExecutor() {
        if (executor != null)
        //todo:needs to be decoupled from service at execution
        {
            return executor;
        } else
            executor = Executors.newFixedThreadPool(3, new DaemonThreadFactory());
        return executor;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }


    public HandlerResolver getHandlerResolver() {
        return handlerResolver;
    }

    public void setHandlerResolver(HandlerResolver resolver) {
        handlerResolver = resolver;
    }

    public Object getPort(QName portName, Class portInterface)
        throws WebServiceException {
        Object seiProxy = createEndpointIFBaseProxy(portName, portInterface);
        seiProxies.add(seiProxy);
        if (portName != null) {
            addPort(portName);
        }

        return seiProxy;
    }

    public Object getPort(Class portInterface) throws WebServiceException {
        return createEndpointIFBaseProxy(null, portInterface);
    }


    public void addPort(QName portName, String bindingId,
                        String endpointAddress) throws WebServiceException {

        if (!dispatchPorts.containsKey(portName)) {
            dispatchPorts.put(portName, new PortInfoBase(endpointAddress,
                portName, bindingId));
        } else
            throw new WebServiceException("Port " + portName.toString() + " already exists can not create a port with the same name.");
        // need to add port to list for HandlerRegistry
        addPort(portName);
    }


    public <T> Dispatch<T> createDispatch(QName qName, Class<T>  aClass, Service.Mode mode) throws WebServiceException {
        //Note: may not be the most performant way to do this- needs review
        return DispatchFactory.createDispatch(qName, aClass, mode, this, masterPipe, getBinding(qName));
    }

    public String getEndpointAddress(QName qName) {
        PortInfoBase dispatchPort = dispatchPorts.get(qName);
        return dispatchPort.getTargetEndpoint();

    }

    public BindingImpl getBinding(QName qName) {
        PortInfoBase dispatchPort = dispatchPorts.get(qName);
        return getBindingforProvider(qName, dispatchPort.getBindingId());

    }

    public Dispatch<Object> createDispatch(QName qName, JAXBContext jaxbContext, Service.Mode mode) throws WebServiceException {
        return new JAXBDispatch(qName, jaxbContext, mode, this, masterPipe, getBinding(qName));
    }

    public QName getServiceName() {
        return serviceContext.getServiceName();
    }

    public Iterator<QName> getPorts() throws WebServiceException {
        if (ports == null)
            populatePorts();

        if (ports.size() == 0)
            throw noWsdlException();
        return ports.iterator();
    }

    public URL getWSDLDocumentLocation() {
        return getWsdlLocation();
    }

    protected void addPorts(QName[] ports) {
        if (ports != null) {
            for (QName port : ports) {
                addPort(port);
            }
        }
    }

    private void populatePorts() {
        if (ports == null)
            ports = new HashSet<QName>();

        WSDLContext wscontext = serviceContext.getWsdlContext();

        if (serviceContext.getServiceName() == null) {
            if (wscontext != null) {
                serviceContext.setServiceName(wscontext.getFirstServiceName());
            }
        }

        if (wscontext != null) {
            QName serviceName = serviceContext.getServiceName();
            Set<QName> knownPorts =
                wscontext.getPortsAsSet(serviceName);
            if (knownPorts != null) {
                QName[] portz = knownPorts.toArray(
                    new QName[knownPorts.size()]);
                addPorts(portz);
                for (QName port : portz) {
                    String endpoint =
                        wscontext.getEndpoint(serviceName, port);
                    String bid = wscontext.getWsdlBinding(serviceName, port)
                        .getBindingId();
                    dispatchPorts.put(port,
                        new PortInfoBase(endpoint, port, bid));
                }
            }
        }
    }

    protected void addPort(QName port) {
        if (ports == null)
            populatePorts();
        ports.add(port);
    }

    protected WebServiceException noWsdlException() {
        return new WebServiceException("dii.service.no.wsdl.available");
    }

    private Object createEndpointIFBaseProxy(QName portName, Class portInterface) throws WebServiceException {

        processServiceContext(portName, portInterface);
        if (portName == null) {
            portName = serviceContext.getEndpointIFContext(portInterface.getName()).getPortName();
        }
        if (!serviceContext.getWsdlContext().contains(getServiceName(), portName))
        {
            throw new WebServiceException("Port " + portName + "is not found in service " + serviceContext.getServiceName());
        }

        return buildEndpointIFProxy(portName, portInterface);
    }

    protected HashSet<QName> getPortsAsSet() {
        if (ports == null)
            populatePorts();
        return ports;
    }

    /*
     * Set the binding on the binding provider. Called by the service
     * class when creating the binding provider.
     */
    protected void setBindingOnProvider(InternalBindingProvider provider,
                                        QName portName, String bindingId) {

        // get handler chain
        List<Handler> handlerChain = null;
        if (getHandlerResolver() != null && getServiceName() != null) {
            PortInfo portInfo = new PortInfoImpl(bindingId.toString(),
                portName, getServiceName());
            handlerChain = getHandlerResolver().getHandlerChain(portInfo);
        } else {
            handlerChain = new ArrayList<Handler>();
        }

        // create binding
        if (bindingId.toString().equals(SOAPBinding.SOAP11HTTP_BINDING) ||
            bindingId.toString().equals(SOAPBinding.SOAP12HTTP_BINDING)) {
            SOAPBindingImpl bindingImpl = new SOAPBindingImpl(handlerChain,
                bindingId.toString(), getServiceName());
//<<<<<<< WSServiceDelegate.java
//
//            if (serviceContext.getRoles() != null) {
//                bindingImpl.setRoles(serviceContext.getRoles());

            if (serviceContext.getRoles(portName) != null) {
                bindingImpl.setRoles(serviceContext.getRoles(portName));
            }
            provider._setBinding(bindingImpl);
        } else if (bindingId.toString().equals(HTTPBinding.HTTP_BINDING)) {
            provider._setBinding(new HTTPBindingImpl(handlerChain));
        }
    }

/*
     * Set the binding on the binding provider. Called by the service
     * class when creating the binding provider.
     */

    /* Todo: temp for now just trying to get something working -kw **/
    private BindingImpl getBindingforProvider(QName portName, String bindingId) {

        // get handler chain
        List<Handler> handlerChain = null;
        if (getHandlerResolver() != null && getServiceName() != null) {
            PortInfo portInfo = new PortInfoImpl(bindingId.toString(),
                portName, getServiceName());
            handlerChain = getHandlerResolver().getHandlerChain(portInfo);
        } else {
            handlerChain = new ArrayList<Handler>();
        }

        // create binding
        if (bindingId.toString().equals(SOAPBinding.SOAP11HTTP_BINDING) ||
            bindingId.toString().equals(SOAPBinding.SOAP12HTTP_BINDING)) {
            SOAPBindingImpl bindingImpl = new SOAPBindingImpl(handlerChain,
                bindingId.toString(), getServiceName());

            if (serviceContext.getRoles(portName) != null) {
                bindingImpl.setRoles(serviceContext.getRoles(portName));
            }
            return bindingImpl;
            //provider._setBinding(bindingImpl);
        } else if (bindingId.toString().equals(HTTPBinding.HTTP_BINDING)) {
            return new HTTPBindingImpl(handlerChain);
            //provider._setBinding(new HTTPBindingImpl(handlerChain));
        }
        return null;
    }


    private Dispatch createDispatchClazz(QName port, Class clazz, Service.Mode mode) throws WebServiceException {
        PortInfoBase dispatchPort = dispatchPorts.get(port);
        if (dispatchPort != null) {
            DispatchBase dBase = new DispatchBase(dispatchPort, clazz, mode, this);
            setBindingOnProvider(dBase, port, dBase._getBindingId());
            return dBase;
        } else {
            throw new WebServiceException("Port must be defined in order to create Dispatch");
        }
    }

    private Dispatch createDispatchJAXB(QName port, JAXBContext jaxbContext, Service.Mode mode) throws WebServiceException {
        PortInfoBase dispatchPort = dispatchPorts.get(port);
        if (dispatchPort != null) {
            DispatchBase dBase = new DispatchBase(dispatchPort, jaxbContext, mode, this);
            setBindingOnProvider(dBase, port, dBase._getBindingId());
            return dBase;
        } else {
            throw new WebServiceException("Port must be defined in order to create Dispatch");
        }
    }

    private URL getWsdlLocation() {
        return serviceContext.getWsdlContext().getWsdlLocation();
    }

    private Object buildEndpointIFProxy(QName portQName, Class portInterface)
        throws WebServiceException {

        EndpointIFContext eif = completeEndpointIFContext(serviceContext, portQName, portInterface);

        //apply parameter bindings
        RuntimeModel model = eif.getRuntimeContext().getModel();
        if (portQName != null) {
            com.sun.xml.ws.wsdl.parser.Binding binding = serviceContext.getWsdlContext().getWsdlBinding(serviceContext.getServiceName(), portQName);
            eif.setBindingID(binding.getBindingId());
            ((AbstractRuntimeModelImpl)model).applyParameterBinding(binding);

        }

        //needs cleaning up
        EndpointIFInvocationHandler handler =
            new EndpointIFInvocationHandler(portInterface,
                eif, this, getServiceName()); //need handler registry passed in here
        setBindingOnProvider(handler, portQName, handler._getBindingId());

        Object proxy = Proxy.newProxyInstance(portInterface.getClassLoader(),
            new Class[]{
                portInterface, BindingProvider.class,
                BindingProviderProperties.class,
                com.sun.xml.ws.spi.runtime.StubBase.class
            }, handler);
        handler.setProxy(proxy);
        return (BindingProvider) proxy;
    }

    private EndpointIFContext completeEndpointIFContext(ServiceContext serviceContext, QName portQName, Class portInterface) {

        EndpointIFContext context = serviceContext.getEndpointIFContext(portInterface.getName());
        WSDLContext wscontext = serviceContext.getWsdlContext();
        if (wscontext != null) {
            String endpoint = wscontext.getEndpoint(serviceContext.getServiceName(), portQName);
            String bindingID = wscontext.getBindingID(
                serviceContext.getServiceName(), portQName);
            context.setServiceName(serviceContext.getServiceName());
            context.setPortInfo(portQName, endpoint, bindingID);
        }
        return context;
    }


    class DaemonThreadFactory implements ThreadFactory {
        public Thread newThread(Runnable r) {
            Thread daemonThread = new Thread(r);
            daemonThread.setDaemon(Boolean.TRUE);
            return daemonThread;
        }
    }
}


