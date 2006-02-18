/*
 * Copyright (c) 2005 Sun Microsystems Corporation. All Rights Reserved.
 */
package com.sun.xml.ws.client;

import com.sun.xml.ws.api.EndpointAddress;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.WSService;
import com.sun.xml.ws.api.model.wsdl.WSDLModel;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.PipelineAssembler;
import com.sun.xml.ws.api.pipe.PipelineAssemblerFactory;
import com.sun.xml.ws.api.pipe.Stubs;
import com.sun.xml.ws.binding.BindingImpl;
import com.sun.xml.ws.binding.http.HTTPBindingImpl;
import com.sun.xml.ws.binding.soap.SOAPBindingImpl;
import com.sun.xml.ws.binding.soap.SOAPHTTPBindingImpl;
import com.sun.xml.ws.client.sei.SEIStub;
import com.sun.xml.ws.handler.HandlerResolverImpl;
import com.sun.xml.ws.handler.PortInfoImpl;
import com.sun.xml.ws.model.AbstractSEIModelImpl;
import com.sun.xml.ws.model.RuntimeModeler;
import com.sun.xml.ws.model.SOAPSEIModel;
import com.sun.xml.ws.model.wsdl.WSDLBoundPortTypeImpl;
import com.sun.xml.ws.model.wsdl.WSDLPortImpl;
import com.sun.xml.ws.model.wsdl.WSDLServiceImpl;
import com.sun.xml.ws.util.HandlerAnnotationInfo;
import com.sun.xml.ws.util.HandlerAnnotationProcessor;
import com.sun.xml.ws.util.xml.XmlUtil;
import com.sun.xml.ws.wsdl.WSDLContext;

import javax.xml.bind.JAXBContext;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.HandlerResolver;
import javax.xml.ws.http.HTTPBinding;
import javax.xml.ws.soap.SOAPBinding;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
 * @see Executor
 * @since JAX-WS 2.0
 */
public class WSServiceDelegate extends WSService {


    /**
     * All ports.
     *
     * <p>
     * This includes ports statically known to WSDL, as well as
     * ones that are dynamically added
     * through {@link #addPort(QName, String, String)}.
     *
     * For statically known ports we'll have {@link SEIPortInfo}.
     * For dynamically added ones we'll have {@link PortInfo}.
     */
    private final Map<QName,PortInfo> ports = new HashMap<QName,PortInfo>();

    private HandlerResolver handlerResolver;

    /**
     * Parsed WSDL.
     *
     * <p>
     * If a WSDL is given in the constructor, this field is set during the constructor.
     * Sometimes the constructor doesn't give this information, and we may get this
     * when {@link #getPort(QName, Class)} or {@link #getPort(Class)} is invoked.
     * (Is such a case really called for in the spec?)
     */
    private WSDLContext wsdlContext;

    private final Class<? extends Service> serviceClass;

    /**
     * Name of the service for which this {@link WSServiceDelegate} is created for.
     * Always non-null.
     */
    private final QName serviceName;

    /**
     * Information about SEI, keyed by their interface type.
     */
    private final Map<Class,SEIPortInfo> seiContext = new HashMap<Class,SEIPortInfo>();

    private final HashMap<QName,Set<String>> rolesMap = new HashMap<QName,Set<String>>();

    private Executor executor;

    /**
     * The WSDL service that this {@link Service} object represents.
     *
     * <p>
     * This field is null iff no WSDL is given to {@link Service}.
     * See {@link #wsdlContext} why this isn't final.
     * TODO: is this really a supported scenario?
     */
    private WSDLServiceImpl wsdlService;


    public WSServiceDelegate(URL wsdlDocumentLocation, QName serviceName, Class<? extends Service> serviceClass) {
        this.serviceName = serviceName;
        this.serviceClass = serviceClass;

        if (serviceClass != Service.class) {
            SCAnnotations serviceCAnnotations = new SCAnnotations(serviceClass);

            if(wsdlDocumentLocation!=null)
                wsdlDocumentLocation = serviceCAnnotations.wsdlLocation;

            if(wsdlDocumentLocation!=null)
                parseWSDL(wsdlDocumentLocation);

            for (Class clazz : serviceCAnnotations.classes)
                addSEI(clazz);
        } else {
            if(wsdlDocumentLocation!=null)
                parseWSDL(wsdlDocumentLocation);
        }
    }

    /**
     * Parses the WSDL and builds {@link WSDLModel}.
     *
     * <p>
     * TODO: the only reason this method isn't a part of the constructor is because
     * the code was written such a way that {@link #getPort(Class)} can inject a WSDL
     * into a {@link Service} that was created without one. Is it really a valid scenario?
     */
    private void parseWSDL(URL wsdlDocumentLocation) {
        wsdlContext = new WSDLContext(wsdlDocumentLocation, XmlUtil.createDefaultCatalogResolver());
        wsdlService = wsdlContext.getWSDLModel().getService(this.serviceName);

        // fill in statically known ports
        for (WSDLPortImpl port : wsdlContext.getPorts(serviceName) ) {
            ports.put(port.getName(),
                new PortInfo(this, port.getAddress(), port.getName(), port.getBinding().getBindingId()));
        }
    }

    public Executor getExecutor() {
        if (executor != null) {
            return executor;
        } else
            executor = Executors.newCachedThreadPool(new DaemonThreadFactory());
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

    public <T> T getPort(QName portName, Class<T> portInterface) throws WebServiceException {
        if(portName==null || portInterface==null)
            throw new IllegalArgumentException();

        return createEndpointIFBaseProxy(portName, portInterface);
    }

    public <T> T getPort(Class<T> portInterface) throws WebServiceException {
        // pick the port name
        QName portName = seiContext.get(portInterface).portName;
        return getPort(portName, portInterface);
    }


    public void addPort(QName portName, String bindingId, String endpointAddress) throws WebServiceException {
        if (!ports.containsKey(portName)) {
            ports.put(portName,
                new PortInfo(this, EndpointAddress.create(endpointAddress), portName, bindingId));
        } else
            throw new WebServiceException("WSDLPort " + portName.toString() + " already exists can not create a port with the same name.");
    }


    public <T> Dispatch<T> createDispatch(QName portName, Class<T>  aClass, Service.Mode mode) throws WebServiceException {
        PortInfo port = ports.get(portName);
        BindingImpl binding = port.createBinding();
        return Stubs.createDispatch(portName, this, binding, aClass, mode, createPipeline(port,binding));
    }

    /**
     * Creates a new pipeline for the given port name.
     */
    private Pipe createPipeline(PortInfo portInfo, WSBinding binding) {
        String bindingId = portInfo.bindingId;

        PipelineAssembler assembler = PipelineAssemblerFactory.create(Thread.currentThread().getContextClassLoader(), bindingId);
        if(assembler==null)
            throw new WebServiceException("Unable to process bindingID="+bindingId);    // TODO: i18n
        return assembler.createClient(
            portInfo.targetEndpoint,
            portInfo.getWSDLModel(),
            this,binding);
    }

    public EndpointAddress getEndpointAddress(QName qName) {
        return ports.get(qName).targetEndpoint;

    }

    public Dispatch<Object> createDispatch(QName portName, JAXBContext jaxbContext, Service.Mode mode) throws WebServiceException {
        PortInfo port = ports.get(portName);
        BindingImpl binding = port.createBinding();
        return Stubs.createJAXBDispatch(portName, this, binding, jaxbContext, mode,
            createPipeline(port,binding));
    }

    public QName getServiceName() {
        return serviceName;
    }

    public Iterator<QName> getPorts() throws WebServiceException {
        // KK: the spec seems to be ambigous about whether
        // this returns ports that are dynamically added or not.
        if (ports.isEmpty())
            throw new WebServiceException("dii.service.no.wsdl.available");
        return ports.keySet().iterator();
    }

    public URL getWSDLDocumentLocation() {
        return wsdlContext.getWsdlLocation();
    }

    private <T> T createEndpointIFBaseProxy(QName portName, Class<T> portInterface) throws WebServiceException {
        if (!wsdlContext.contains(serviceName, portName)) {
            throw new WebServiceException("WSDLPort " + portName + "is not found in service " + serviceName);
        }

        SEIPortInfo eif = seiContext.get(portInterface);

        //apply parameter bindings
        SOAPSEIModel model = eif.model;
        if (portName != null) {
            WSDLBoundPortTypeImpl binding = getPortModel(portName).getBinding();
            model.applyParameterBinding(binding);
        }

        BindingImpl binding = eif.createBinding();
        SEIStub pis = new SEIStub(this, binding, model, createPipeline(eif,binding));

        return portInterface.cast(Proxy.newProxyInstance(portInterface.getClassLoader(),
            new Class[]{ portInterface, BindingProvider.class }, pis));
    }

    /**
     * Determines the binding of the given port.
     */
    protected BindingImpl createBinding(QName portName, String bindingId) {

        // get handler chain
        List<Handler> handlerChain;
        if (handlerResolver != null) {
            javax.xml.ws.handler.PortInfo portInfo = new PortInfoImpl(bindingId, portName, serviceName);
            handlerChain = handlerResolver.getHandlerChain(portInfo);
        } else {
            handlerChain = new ArrayList<Handler>();
        }

        // create binding
        if (bindingId.equals(SOAPBinding.SOAP11HTTP_BINDING) ||
            bindingId.equals(SOAPBinding.SOAP12HTTP_BINDING)) {
            SOAPBindingImpl bindingImpl = new SOAPHTTPBindingImpl(
                handlerChain, SOAPVersion.fromHttpBinding(bindingId), serviceName);

            Set<String> roles = rolesMap.get(portName);
            if (roles != null) {
                bindingImpl.setRoles(roles);
            }
            return bindingImpl;
        } else if (bindingId.equals(HTTPBinding.HTTP_BINDING)) {
            return new HTTPBindingImpl(handlerChain);
        }

        // TODO: what does this mean?
        // is this a configuration error or assertion failure?
        throw new Error();
    }

    /**
     * Obtains a {@link WSDLPortImpl} with error check.
     * @return
     *      guaranteed to be non-null.
     */
    private WSDLPortImpl getPortModel(QName portName) {
        WSDLPortImpl port = wsdlService.get(portName);
        if(port==null)
            throw new WebServiceException("No ports found for service " + serviceName);
        return port;
    }

    /**
     * Contributes to the construction of {@link WSServiceDelegate} by filling in
     * {@link SEIPortInfo} about a given SEI (linked from the {@link Service}-derived class.)
     */
    //todo: valid port in wsdl
    private void addSEI(Class portInterface) throws WebServiceException {
        SEIPortInfo spi = seiContext.get(portInterface);
        if (spi != null)    return;


        QName portName = guessPortName(portInterface);
        if (portName == null) {
            portName = wsdlContext.getPortName();
        }

        //todo:use SCAnnotations and put in map
        WSDLPortImpl wsdlPort = wsdlService.get(portName);
        // TODO: error check against wsdlPort==null
        RuntimeModeler modeler = new RuntimeModeler(portInterface,serviceName,wsdlPort,false);
        modeler.setPortName(portName);
        AbstractSEIModelImpl model = modeler.buildRuntimeModel();

        spi = new SEIPortInfo(this,wsdlPort.getAddress(),portName,
            wsdlPort.getBinding().getBindingId(),portInterface, (SOAPSEIModel)model,wsdlPort);
        seiContext.put(spi.sei,spi);
        ports.put(spi.portName,spi);

        // get handler information
        HandlerAnnotationInfo chainInfo =
            HandlerAnnotationProcessor.buildHandlerInfo(portInterface,
                model.getServiceQName(), model.getPortName(), spi.createBinding() );

        if (chainInfo != null) {
            if(handlerResolver==null)
                handlerResolver = new HandlerResolverImpl();

            // the following cast to HandlerResolverImpl always succeed, as the addPort method
            // is only used during the construction of WSServiceDelegate
            ((HandlerResolverImpl)handlerResolver).setHandlerChain(new PortInfoImpl(
                wsdlPort.getBinding().getBindingId(),
                model.getPortName(),
                model.getServiceQName()),
                chainInfo.getHandlers());
            rolesMap.put(portName,chainInfo.getRoles());
        }
    }

    /**
     * Try to obtain the port name of the given SEI by using
     * {@link WebEndpoint} annotation on {@link #serviceClass}.
     *
     * @return
     *      return null if this method fails to detect the port name.
     */
    private QName guessPortName(Class<?> sei) {
        if (serviceClass == null)
            return null;

        WebServiceClient wsClient = serviceClass.getAnnotation(WebServiceClient.class);
        for (Method method : serviceClass.getMethods()) {
            if (method.getDeclaringClass()!=serviceClass) {
                continue;
            }
            WebEndpoint webEndpoint = method.getAnnotation(WebEndpoint.class);
            if (webEndpoint == null) {
                continue;
            }
            if (method.getGenericReturnType()==sei) {
                if (method.getName().startsWith("get")) {
                    return new QName(wsClient.targetNamespace(), webEndpoint.name());
                }
            }
        }
        return null;
    }

    class DaemonThreadFactory implements ThreadFactory {
        public Thread newThread(Runnable r) {
            Thread daemonThread = new Thread(r);
            daemonThread.setDaemon(Boolean.TRUE);
            return daemonThread;
        }
    }
}


