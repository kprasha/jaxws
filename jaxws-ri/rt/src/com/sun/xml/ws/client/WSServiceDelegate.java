/*
 * Copyright (c) 2005 Sun Microsystems Corporation. All Rights Reserved.
 */
package com.sun.xml.ws.client;

import com.sun.org.apache.xml.internal.resolver.tools.CatalogResolver;
import com.sun.xml.ws.api.WSService;
import com.sun.xml.ws.api.model.SEIModel;
import com.sun.xml.ws.api.model.wsdl.WSDLModel;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.model.wsdl.WSDLService;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.PipelineAssembler;
import com.sun.xml.ws.api.pipe.PipelineAssemblerFactory;
import com.sun.xml.ws.api.pipe.Stubs;
import com.sun.xml.ws.binding.BindingImpl;
import com.sun.xml.ws.binding.http.HTTPBindingImpl;
import com.sun.xml.ws.binding.soap.SOAPBindingImpl;
import com.sun.xml.ws.client.dispatch.rearch.jaxb.JAXBDispatch;
import com.sun.xml.ws.client.port.PortInterfaceStub;
import com.sun.xml.ws.handler.HandlerResolverImpl;
import com.sun.xml.ws.handler.PortInfoImpl;
import com.sun.xml.ws.model.AbstractSEIModelImpl;
import com.sun.xml.ws.model.RuntimeModeler;
import com.sun.xml.ws.model.wsdl.WSDLBoundPortTypeImpl;
import com.sun.xml.ws.server.RuntimeContext;
import com.sun.xml.ws.util.HandlerAnnotationInfo;
import com.sun.xml.ws.util.HandlerAnnotationProcessor;
import com.sun.xml.ws.util.JAXWSUtils;
import com.sun.xml.ws.util.xml.XmlUtil;
import com.sun.xml.ws.wsdl.WSDLContext;
import org.xml.sax.EntityResolver;

import javax.jws.WebService;
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
import javax.xml.ws.handler.PortInfo;
import javax.xml.ws.http.HTTPBinding;
import javax.xml.ws.soap.SOAPBinding;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
     */
    private final Map<QName,PortInfoBase> ports = new HashMap<QName, PortInfoBase>();

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
     * Service endpoint interface keyed by their interface type.
     */
    private final Map<Class,EndpointIFContext> seiContext = new HashMap<Class,EndpointIFContext>();

    private final HashMap<QName,Set<String>> rolesMap = new HashMap<QName,Set<String>>();

    private Executor executor;

    private final HashSet<Object> seiProxies = new HashSet<Object>();

    /**
     * {@link CatalogResolver} to check META-INF/jax-ws-catalog.xml.
     */
    private final EntityResolver entityResolver = XmlUtil.createDefaultCatalogResolver();

    /**
     * The WSDL service that this {@link Service} object represents.
     *
     * <p>
     * This field is null iff no WSDL is given to {@link Service}.
     * See {@link #wsdlContext} why this isn't final.
     * TODO: is this really a supported scenario?
     */
    private WSDLService wsdlService;


    public WSServiceDelegate(URL wsdlDocumentLocation, QName serviceName, Class<? extends Service> serviceClass) {
        this.serviceName = serviceName;
        this.serviceClass = serviceClass;

        if (serviceClass != Service.class) {
            SCAnnotations serviceCAnnotations = new SCAnnotations(serviceClass);

            if(wsdlDocumentLocation!=null)
                wsdlDocumentLocation = serviceCAnnotations.wsdlLocation;

            for (Class clazz : serviceCAnnotations.classes) {
                addSEI(clazz);
            }
        }

        if(wsdlDocumentLocation!=null)
            parseWSDL(wsdlDocumentLocation);

        populatePorts();
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
        wsdlContext = new WSDLContext(wsdlDocumentLocation, entityResolver);
        wsdlService = wsdlContext.getWSDLModel().getService(this.serviceName);
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

    public <T> T getPort(QName portName, Class<T> portInterface) throws WebServiceException {
        if(portName==null || portInterface==null)
            throw new IllegalArgumentException();

        T seiProxy = createEndpointIFBaseProxy(portName, portInterface);
        seiProxies.add(seiProxy);

        return seiProxy;
    }

    public <T> T getPort(Class<T> portInterface) throws WebServiceException {
        // pick the port name
        QName portName = getEndpointIFContext(portInterface).getPortName();
        return getPort(portName, portInterface);
    }


    public void addPort(QName portName, String bindingId,
                        String endpointAddress) throws WebServiceException {

        if (!ports.containsKey(portName)) {
            ports.put(portName, new PortInfoBase(endpointAddress,
                portName, bindingId));
        } else
            throw new WebServiceException("WSDLPort " + portName.toString() + " already exists can not create a port with the same name.");
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
        return assembler.createClient(port,this,binding);
    }

    public String getEndpointAddress(QName qName) {
        PortInfoBase dispatchPort = ports.get(qName);
        return dispatchPort.getTargetEndpoint();

    }

    public BindingImpl getBinding(QName portName) {
        PortInfoBase dispatchPort = ports.get(portName);
        return createBinding(portName, dispatchPort.getBindingId());

    }

    public Dispatch<Object> createDispatch(QName portName, JAXBContext jaxbContext, Service.Mode mode) throws WebServiceException {
        BindingImpl binding = getBinding(portName);
        return new JAXBDispatch(portName, jaxbContext, mode, this, createPipeline(portName,binding), binding);
    }

    public QName getServiceName() {
        return serviceName;
    }

    public Iterator<QName> getPorts() throws WebServiceException {
        // KK: the spec seems to be ambigous about whether
        // this returns ports that are dynamically added or not.
        if (ports.isEmpty())
            throw noWsdlException();
        return ports.keySet().iterator();
    }

    public URL getWSDLDocumentLocation() {
        return wsdlContext.getWsdlLocation();
    }

    private void populatePorts() {
        if (wsdlContext != null) {
            for (WSDLPort port : wsdlContext.getPorts(serviceName) ) {
                QName portName = port.getName();

                String endpoint =
                    wsdlContext.getEndpoint(serviceName, portName);
                String bid = wsdlContext.getWsdlBinding(serviceName, portName)
                    .getBindingId();
                ports.put(portName,
                    new PortInfoBase(endpoint, portName, bid));
            }
        }
    }

    private WebServiceException noWsdlException() {
        return new WebServiceException("dii.service.no.wsdl.available");
    }

    private <T> T createEndpointIFBaseProxy(QName portName, Class<T> portInterface) throws WebServiceException {
        if (wsdlContext==null) {
            URL wsdlLocation;
            try {
                wsdlLocation = new URL(JAXWSUtils.getFileOrURLName(getWSDLLocation(portInterface)));
            } catch (MalformedURLException e) {
                throw new WebServiceException(e);
            }

            parseWSDL(wsdlLocation);
        }

        if (!seiContext.isEmpty())
            addSEI(portInterface);  // KK: this if block doesn't make sense to me. why not just always call addSEI?

        if (!wsdlContext.contains(getServiceName(), portName)) {
            throw new WebServiceException("WSDLPort " + portName + "is not found in service " + serviceName);
        }

        return buildEndpointIFProxy(portName, portInterface);
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


    private <T> T buildEndpointIFProxy(QName portName, Class<T> portInterface)
        throws WebServiceException {

        EndpointIFContext eif = completeEndpointIFContext(portName, portInterface);

        //apply parameter bindings
        SEIModel model = eif.getRuntimeContext().getModel();
        if (portName != null) {
            WSDLBoundPortTypeImpl binding = (WSDLBoundPortTypeImpl) wsdlContext.getWsdlBinding(serviceName, portName);
            eif.setBindingID(binding.getBindingId());
            ((AbstractSEIModelImpl)model).applyParameterBinding(binding);

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

    private EndpointIFContext completeEndpointIFContext(QName portQName, Class portInterface) {
        EndpointIFContext context = getEndpointIFContext(portInterface);
        if (wsdlContext != null) {
            String endpoint = wsdlContext.getEndpoint(serviceName, portQName);
            String bindingID = wsdlContext.getBindingID(serviceName, portQName);
            context.setServiceName(serviceName);
            context.setPortInfo(portQName, endpoint, bindingID);
        }
        return context;
    }

    public EndpointIFContext getEndpointIFContext(Class sei) {
        return seiContext.get(sei);
    }

    /**
     * Contributes to the construction of {@link WSServiceDelegate} by filling in
     * {@link EndpointIFContext} about a given SEI (linked from the {@link Service}-derived class.)
     */
    //todo: valid port in wsdl
    void addSEI(Class portInterface) throws WebServiceException {
        EndpointIFContext eifc = getEndpointIFContext(portInterface);
        if ((eifc == null) || (eifc.getRuntimeContext() == null)) {

            if (eifc == null) {
                eifc = new EndpointIFContext(portInterface);
                seiContext.put(eifc.getSei(),eifc);
            }

            //toDo:
            QName serviceName = getServiceName();
            QName portName = eifc.getPortName();
            if (serviceClass != null) {
                if (serviceName == null)
                    serviceName = getServiceName(serviceClass);
                if (portName == null)
                    portName = getPortName(portInterface, serviceClass);
            }

            if (portName == null) {
                portName = wsdlContext.getPortName();
            }

            //todo:use SCAnnotations and put in map
            String bindingId = wsdlContext.getBindingID(
                serviceName, portName);
            RuntimeModeler modeler = new RuntimeModeler(portInterface,
                serviceName, bindingId);
            modeler.setPortName(portName);
            AbstractSEIModelImpl model = modeler.buildRuntimeModel();

            eifc.setRuntimeContext(new RuntimeContext(model));

            // get handler information
            HandlerAnnotationInfo chainInfo =
                HandlerAnnotationProcessor.buildHandlerInfo(portInterface,
                    model.getServiceQName(), model.getPortName(), bindingId);

            if (chainInfo != null) {
                if(handlerResolver==null)
                    handlerResolver = new HandlerResolverImpl();

                // the following cast to HandlerResolverImpl always succeed, as the addPort method
                // is only used during the construction of WSServiceDelegate
                ((HandlerResolverImpl)handlerResolver).setHandlerChain(new PortInfoImpl(
                    bindingId,
                    model.getPortName(),
                    model.getServiceQName()),
                    chainInfo.getHandlers());
                rolesMap.put(portName,chainInfo.getRoles());
            }
        }
    }

    private static QName getServiceName(Class<?> serviceInterface) {
        WebServiceClient wsClient = serviceInterface.getAnnotation(WebServiceClient.class);
        QName serviceName = null;
        if (wsClient != null) {
            String name = wsClient.name();
            String namespace = wsClient.targetNamespace();
            serviceName = new QName(namespace, name);
        }
        return serviceName;
    }

    private static QName getPortName(Class<?> portInterface, Class<?> serviceInterface) {
        QName portName = null;
        WebServiceClient wsClient = serviceInterface.getAnnotation(WebServiceClient.class);
        for (Method method : serviceInterface.getMethods()) {
            if (method.getDeclaringClass()!=serviceInterface) {
                continue;
            }
            WebEndpoint webEndpoint = method.getAnnotation(WebEndpoint.class);
            if (webEndpoint == null) {
                continue;
            }
            if (method.getGenericReturnType()==portInterface) {
                if (method.getName().startsWith("get")) {
                    portName = new QName(wsClient.targetNamespace(), webEndpoint.name());
                    break;
                }
            }
        }
        return portName;
    }

    /**
     * Utility method to get wsdlLocation attribute from @WebService annotation on sei.
     *
     * @return the URL of the location of the WSDL for the sei, or null if none was found.
     */
//this will change
    private static String getWSDLLocation(Class<?> sei) {
        WebService ws = sei.getAnnotation(WebService.class);
        if (ws == null)
            return null;
        return ws.wsdlLocation();
    }


    class DaemonThreadFactory implements ThreadFactory {
        public Thread newThread(Runnable r) {
            Thread daemonThread = new Thread(r);
            daemonThread.setDaemon(Boolean.TRUE);
            return daemonThread;
        }
    }
}


