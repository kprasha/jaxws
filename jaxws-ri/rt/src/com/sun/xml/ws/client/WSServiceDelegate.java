/*
 * Copyright (c) 2005 Sun Microsystems Corporation. All Rights Reserved.
 */
package com.sun.xml.ws.client;

import com.sun.org.apache.xml.internal.resolver.tools.CatalogResolver;
import com.sun.xml.ws.api.WSService;
import com.sun.xml.ws.api.model.RuntimeModel;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.model.wsdl.WSDLModel;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.PipelineAssembler;
import com.sun.xml.ws.api.pipe.PipelineAssemblerFactory;
import com.sun.xml.ws.api.pipe.Stubs;
import com.sun.xml.ws.binding.BindingImpl;
import com.sun.xml.ws.binding.http.HTTPBindingImpl;
import com.sun.xml.ws.binding.soap.SOAPBindingImpl;
import com.sun.xml.ws.client.dispatch.rearch.jaxb.JAXBDispatch;
import com.sun.xml.ws.client.port.PortInterfaceStub;
import com.sun.xml.ws.handler.PortInfoImpl;
import com.sun.xml.ws.model.AbstractRuntimeModelImpl;
import com.sun.xml.ws.model.wsdl.WSDLBoundPortTypeImpl;
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
import java.util.Map;
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
 * @see Executor
 * @since JAX-WS 2.0
 */
public class WSServiceDelegate extends WSService {

    protected static final String GET = "get";


    protected HashSet<QName> ports;

    /**
     * Ports dynamically added through {@link #addPort(QName, String, String)}.
     */
    private final Map<QName,PortInfoBase> dispatchPorts = new HashMap<QName, PortInfoBase>();

    protected HandlerResolver handlerResolver;

    protected Object serviceProxy;
    protected URL wsdlLocation;
    protected ServiceContext serviceContext;
    protected Executor executor;
    private final HashSet<Object> seiProxies = new HashSet<Object>();

    /**
     * {@link CatalogResolver} to check META-INF/jax-ws-catalog.xml.
     * Lazily created.
     */
    private EntityResolver entityResolver;

    /**
     * The WSDL service that this {@link Service} object represents.
     *
     * <p>
     * This field is null iff no WSDL is given to {@link Service}.
     */
    private final com.sun.xml.ws.api.model.wsdl.WSDLService wsdlService;


    public WSServiceDelegate(ServiceContext scontext) {
        serviceContext = scontext;
        if (serviceContext.getHandlerResolver() != null) {
            handlerResolver = serviceContext.getHandlerResolver();
        }
        WSDLModel wsdlModel = serviceContext.getWSDLModel();
        wsdlService = (wsdlModel != null)?wsdlModel.getService(serviceContext.getServiceName()):null;
    }

    public WSServiceDelegate(URL wsdlDocumentLocation, QName serviceName, Class serviceClass) {
        this(createServiceContext(wsdlDocumentLocation,serviceName,serviceClass));
        if (ports == null)
            populatePorts();
    }

    private static ServiceContext createServiceContext(URL wsdlDocumentLocation, QName serviceName, Class serviceClass) {
        if (wsdlDocumentLocation != null) {
            return ServiceContextBuilder.build(
                wsdlDocumentLocation, serviceClass, XmlUtil.createDefaultCatalogResolver());
        } else {
            ServiceContext serviceContext = new ServiceContext(XmlUtil.createDefaultCatalogResolver());
            serviceContext.setServiceName(serviceName);
            return serviceContext;
        }
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

    public <T> T getPort(QName portName, Class<T> portInterface)
        throws WebServiceException {
        T seiProxy = createEndpointIFBaseProxy(portName, portInterface);
        seiProxies.add(seiProxy);
        if (portName != null) {
            addPort(portName);
        }

        return seiProxy;
    }

    public <T> T getPort(Class<T> portInterface) throws WebServiceException {
        return createEndpointIFBaseProxy(null, portInterface);
    }


    public void addPort(QName portName, String bindingId,
                        String endpointAddress) throws WebServiceException {

        if (!dispatchPorts.containsKey(portName)) {
            dispatchPorts.put(portName, new PortInfoBase(endpointAddress,
                portName, bindingId));
        } else
            throw new WebServiceException("WSDLPort " + portName.toString() + " already exists can not create a port with the same name.");
        // need to add port to list for HandlerRegistry
        addPort(portName);
    }


    public <T> Dispatch<T> createDispatch(QName portName, Class<T>  aClass, Service.Mode mode) throws WebServiceException {
        //Note: may not be the most performant way to do this- needs review
        BindingImpl binding = getBinding(portName);
        return Stubs.createDispatch(portName, this, binding, aClass, mode,
             createPipeline(portName,binding));
    }

    /**
     * Creates a new pipeline for the given port name.
     */
    private Pipe createPipeline(QName portName, BindingImpl binding) {
        WSDLPort port = null;
        String bindingId = binding.getBindingId();

        if(wsdlService !=null) {
            port = wsdlService.get(portName);
            // TODO: error check for port==null?
        }

        PipelineAssembler assembler = PipelineAssemblerFactory.create(Thread.currentThread().getContextClassLoader(), bindingId);
        if(assembler==null)
            throw new WebServiceException("Unable to process bindingID="+bindingId);    // TODO: i18n
        return assembler.createClient(port,this);
    }

    public String getEndpointAddress(QName qName) {
        PortInfoBase dispatchPort = dispatchPorts.get(qName);
        return dispatchPort.getTargetEndpoint();

    }

    public BindingImpl getBinding(QName portName) {
        PortInfoBase dispatchPort = dispatchPorts.get(portName);
        return createBinding(portName, dispatchPort.getBindingId());

    }

    public Dispatch<Object> createDispatch(QName portName, JAXBContext jaxbContext, Service.Mode mode) throws WebServiceException {
        BindingImpl binding = getBinding(portName);
        return new JAXBDispatch(portName, jaxbContext, mode, this, createPipeline(portName,binding), binding);
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
            for (WSDLPort port : wscontext.getPorts(serviceName) ) {
                QName portName = port.getName();

                addPort(portName);

                String endpoint =
                    wscontext.getEndpoint(serviceName, portName);
                String bid = wscontext.getWsdlBinding(serviceName, portName)
                    .getBindingId();
                dispatchPorts.put(portName,
                    new PortInfoBase(endpoint, portName, bid));
            }
        }
    }

    private void addPort(QName port) {
        if (ports == null)
            populatePorts();
        ports.add(port);
    }

    private WebServiceException noWsdlException() {
        return new WebServiceException("dii.service.no.wsdl.available");
    }

    private <T> T createEndpointIFBaseProxy(QName portName, Class<T> portInterface) throws WebServiceException {

        ServiceContextBuilder.completeServiceContext(serviceContext, portInterface);

        if (portName == null) {
            portName = serviceContext.getEndpointIFContext(portInterface).getPortName();
        }

        if (!serviceContext.getWsdlContext().contains(getServiceName(), portName)) {
            throw new WebServiceException("WSDLPort " + portName + "is not found in service " + serviceContext.getServiceName());
        }

        return buildEndpointIFProxy(portName, portInterface);
    }

    protected HashSet<QName> getPortsAsSet() {
        if (ports == null)
            populatePorts();
        return ports;
    }

    /**
     * Determines the binding of the given port.
     */
    protected BindingImpl createBinding(QName portName, String bindingId) {

        // get handler chain
        List<Handler> handlerChain;
        if (getHandlerResolver() != null && getServiceName() != null) {
            PortInfo portInfo = new PortInfoImpl(bindingId,
                portName, getServiceName());
            handlerChain = getHandlerResolver().getHandlerChain(portInfo);
        } else {
            handlerChain = new ArrayList<Handler>();
        }

        // create binding
        if (bindingId.equals(SOAPBinding.SOAP11HTTP_BINDING) ||
            bindingId.equals(SOAPBinding.SOAP12HTTP_BINDING)) {
            SOAPBindingImpl bindingImpl = new SOAPBindingImpl(handlerChain,
                bindingId, getServiceName());
//<<<<<<< WSServiceDelegate.java
//
//            if (serviceContext.getRoles() != null) {
//                bindingImpl.setRoles(serviceContext.getRoles());

            if (serviceContext.getRoles(portName) != null) {
                bindingImpl.setRoles(serviceContext.getRoles(portName));
            }
            return bindingImpl;
        } else if (bindingId.equals(HTTPBinding.HTTP_BINDING)) {
            return new HTTPBindingImpl(handlerChain);
        }

        // TODO: what does this mean?
        // is this a configuration error or assertion failure?
        throw new Error();
    }


    private URL getWsdlLocation() {
        return serviceContext.getWsdlContext().getWsdlLocation();
    }

    private <T> T buildEndpointIFProxy(QName portName, Class<T> portInterface)
        throws WebServiceException {

        EndpointIFContext eif = completeEndpointIFContext(serviceContext, portName, portInterface);

        //apply parameter bindings
        RuntimeModel model = eif.getRuntimeContext().getModel();
        if (portName != null) {
            WSDLBoundPortTypeImpl binding = (WSDLBoundPortTypeImpl) serviceContext.getWsdlContext().getWsdlBinding(serviceContext.getServiceName(), portName);
            eif.setBindingID(binding.getBindingId());
            ((AbstractRuntimeModelImpl)model).applyParameterBinding(binding);

        }

        BindingImpl binding = createBinding(portName, eif.getBindingID());
        PortInterfaceStub pis = new PortInterfaceStub(this, binding, portInterface,model, createPipeline(portName,binding));

        return portInterface.cast(Proxy.newProxyInstance(portInterface.getClassLoader(),
            new Class[]{
                portInterface, BindingProvider.class,
                BindingProviderProperties.class,
                com.sun.xml.ws.spi.runtime.StubBase.class
            }, pis));
    }

    private EndpointIFContext completeEndpointIFContext(ServiceContext serviceContext, QName portQName, Class portInterface) {

        EndpointIFContext context = serviceContext.getEndpointIFContext(portInterface);
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


