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

package com.sun.xml.ws.client;

import com.sun.istack.NotNull;
import com.sun.istack.Nullable;
import com.sun.xml.ws.Closeable;
import com.sun.xml.ws.addressing.EndpointReferenceUtil;
import com.sun.xml.ws.api.BindingID;
import com.sun.xml.ws.api.EndpointAddress;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.WSService;
import com.sun.xml.ws.api.addressing.WSEndpointReference;
import com.sun.xml.ws.api.client.ContainerResolver;
import com.sun.xml.ws.api.client.PortCreationCallback;
import com.sun.xml.ws.api.client.ServiceInterceptor;
import com.sun.xml.ws.api.client.ServiceInterceptorFactory;
import com.sun.xml.ws.api.client.WSBindingProvider;
import com.sun.xml.ws.api.model.wsdl.WSDLModel;
import com.sun.xml.ws.api.pipe.ClientTubeAssemblerContext;
import com.sun.xml.ws.api.pipe.Stubs;
import com.sun.xml.ws.api.pipe.Tube;
import com.sun.xml.ws.api.pipe.TubelineAssembler;
import com.sun.xml.ws.api.pipe.TubelineAssemblerFactory;
import com.sun.xml.ws.api.server.Container;
import com.sun.xml.ws.api.wsdl.parser.WSDLParserExtension;
import com.sun.xml.ws.binding.BindingImpl;
import com.sun.xml.ws.client.HandlerConfigurator.AnnotationConfigurator;
import com.sun.xml.ws.client.HandlerConfigurator.HandlerResolverImpl;
import com.sun.xml.ws.client.sei.SEIStub;
import com.sun.xml.ws.developer.MemberSubmissionEndpointReference;
import com.sun.xml.ws.model.AbstractSEIModelImpl;
import com.sun.xml.ws.model.RuntimeModeler;
import com.sun.xml.ws.model.SOAPSEIModel;
import com.sun.xml.ws.model.wsdl.WSDLModelImpl;
import com.sun.xml.ws.model.wsdl.WSDLPortImpl;
import com.sun.xml.ws.model.wsdl.WSDLServiceImpl;
import com.sun.xml.ws.resources.ClientMessages;
import com.sun.xml.ws.util.ServiceConfigurationError;
import com.sun.xml.ws.util.ServiceFinder;
import static com.sun.xml.ws.util.xml.XmlUtil.createDefaultCatalogResolver;
import com.sun.xml.ws.wsdl.parser.RuntimeWSDLParser;
import org.xml.sax.SAXException;

import javax.jws.HandlerChain;
import javax.xml.bind.JAXBContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Dispatch;
import javax.xml.ws.EndpointReference;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.handler.HandlerResolver;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * <code>Service</code> objects provide the client view of a Web service.
 *
 * <p><code>Service</code> acts as a factory of the following:
 * <ul>
 * <li>Proxies for a target service endpoint.
 * <li>Instances of <code>javax.xml.ws.Dispatch</code> for
 * dynamic message-oriented invocation of a remote
 * operation.
 * </li>
 *
 * <p>The ports available on a service can be enumerated using the
 * <code>getPorts</code> method. Alternatively, you can pass a
 * service endpoint interface to the unary <code>getPort</code> method
 * and let the runtime select a compatible port.
 *
 * <p>Handler chains for all the objects created by a <code>Service</code>
 * can be set by means of the provided <code>HandlerRegistry</code>.
 *
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
     * <p>
     * This includes ports statically known to WSDL, as well as
     * ones that are dynamically added
     * through {@link #addPort(QName, String, String)}.
     * <p>
     * For statically known ports we'll have {@link SEIPortInfo}.
     * For dynamically added ones we'll have {@link PortInfo}.
     */
    private final Map<QName, PortInfo> ports = new HashMap<QName, PortInfo>();

    /**
     * Whenever we create {@link BindingProvider}, we use this to configure handlers.
     */
    private @NotNull HandlerConfigurator handlerConfigurator = new HandlerResolverImpl(null);

    private final Class<? extends Service> serviceClass;

    /**
     * Name of the service for which this {@link WSServiceDelegate} is created for.
     */
    private final @NotNull QName serviceName;

    /**
     * Information about SEI, keyed by their interface type.
     */
    private final Map<Class,SEIPortInfo> seiContext = new HashMap<Class,SEIPortInfo>();

    private Executor executor;

    /**
     * The WSDL service that this {@link Service} object represents.
     * <p>
     * This field is null iff no WSDL is given to {@link Service}.
     * This fiels can be be null if the service is created without wsdl but later
     * the epr supplies a wsdl that can be parsed.
     */
    private  @Nullable WSDLServiceImpl wsdlService;

    private final Container container;
    /**
     * Multiple {@link ServiceInterceptor}s are aggregated into one.
     */
    /*package*/ final @NotNull ServiceInterceptor serviceInterceptor;


    public WSServiceDelegate(URL wsdlDocumentLocation, QName serviceName, Class<? extends Service> serviceClass) {
        this(
            wsdlDocumentLocation==null ? null : new StreamSource(wsdlDocumentLocation.toExternalForm()),
            serviceName,serviceClass);
    }

    /**
     * @param serviceClass
     *      Either {@link Service}.class or other generated service-derived classes.
     */
    public WSServiceDelegate(@Nullable Source wsdl, @NotNull QName serviceName, @NotNull final Class<? extends Service> serviceClass) {
        //we cant create a Service without serviceName
        if (serviceName == null)
            throw new WebServiceException(ClientMessages.INVALID_SERVICE_NAME_NULL(serviceName));
        this.serviceName = serviceName;
        this.serviceClass = serviceClass;
        this.container = ContainerResolver.getInstance().getContainer();

        // load interceptor
        ServiceInterceptor interceptor = ServiceInterceptorFactory.load(this, Thread.currentThread().getContextClassLoader());
        // backward compatiblity. also pick one up from container
        PortCreationCallback pcc = container.getSPI(PortCreationCallback.class);
        if(pcc!=null)
            interceptor = ServiceInterceptor.aggregate(interceptor,pcc);
        this.serviceInterceptor = interceptor;

        WSDLServiceImpl service=null;
        if (wsdl != null) {
            try {
                URL url = wsdl.getSystemId()==null ? null : new URL(wsdl.getSystemId());
                WSDLModelImpl model = parseWSDL(url, wsdl);
                service = model.getService(this.serviceName);
                if (service == null)
                    throw new WebServiceException(
                        ClientMessages.INVALID_SERVICE_NAME(this.serviceName,
                            buildNameList(model.getServices().keySet())));
                // fill in statically known ports
                for (WSDLPortImpl port : service.getPorts())
                    ports.put(port.getName(), new PortInfo(this, port));
            } catch (MalformedURLException e) {
                throw new WebServiceException(ClientMessages.INVALID_WSDL_URL(wsdl.getSystemId()));
            }
        }
        this.wsdlService = service;

        if (serviceClass != Service.class) {
            //if @HandlerChain present, set HandlerResolver on service context
            HandlerChain handlerChain =
                    AccessController.doPrivileged(new PrivilegedAction<HandlerChain>() {
                        public HandlerChain run() {
                            return serviceClass.getAnnotation(HandlerChain.class);
                        }
                    });
            if (handlerChain != null)
                handlerConfigurator = new AnnotationConfigurator(this);
        }

    }

    /**
     * Parses the WSDL and builds {@link WSDLModel}.
     * @param wsdlDocumentLocation
     *      Either this or <tt>wsdl</tt> parameter must be given.
     *      Null location means the system won't be able to resolve relative references in the WSDL,
     *      So think twice before passing in null.
     */
    private WSDLModelImpl parseWSDL(URL wsdlDocumentLocation, Source wsdl) {
        try {
            return RuntimeWSDLParser.parse(wsdlDocumentLocation, wsdl, createDefaultCatalogResolver(),
                true, ServiceFinder.find(WSDLParserExtension.class).toArray());
        } catch (IOException e) {
            throw new WebServiceException(e);
        } catch (XMLStreamException e) {
            throw new WebServiceException(e);
        } catch (SAXException e) {
            throw new WebServiceException(e);
        } catch (ServiceConfigurationError e) {
            throw new WebServiceException(e);
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
        return handlerConfigurator.getResolver();
    }

    /*package*/ final HandlerConfigurator getHandlerConfigurator() {
        return handlerConfigurator;
    }

    public void setHandlerResolver(HandlerResolver resolver) {
        handlerConfigurator = new HandlerResolverImpl(resolver);
    }

    public <T> T getPort(QName portName, Class<T> portInterface) throws WebServiceException {
        return getPort(portName, portInterface, EMPTY_FEATURES);
    }

    //milestone 2
    public <T> T getPort(QName portName, Class<T> portInterface, WebServiceFeature... features) {
        if (portName == null || portInterface == null)
            throw new IllegalArgumentException();
        addSEI(portName, portInterface);
        return createEndpointIFBaseProxy(null, portName, portInterface, features);
    }

    public <T> T getPort(EndpointReference epr, Class<T> portInterface, WebServiceFeature... features) {
        return getPort(WSEndpointReference.create(epr),portInterface,features);
    }

    public <T> T getPort(WSEndpointReference epr, Class<T> portInterface, WebServiceFeature... features) {
        //get the port specified in EPR
        QName portTypeName = RuntimeModeler.getPortTypeName(portInterface);
        WSEndpointReference.Metadata metadata = epr.getMetaData();
        QName portName = metadata.getPortName();
        //TODO validate service and port in epr and wsdl.
        if (portName == null) {
            //get the first port corresponding to the SEI
            WSDLPortImpl port = wsdlService.getMatchingPort(portTypeName);
            if (port == null)
                throw new WebServiceException(ClientMessages.UNDEFINED_PORT_TYPE(portTypeName));
            portName = port.getName();
        }
        addSEI(portName, portInterface);
        return createEndpointIFBaseProxy(epr,portName,portInterface,features);
    }

    public <T> T getPort(Class<T> portInterface, WebServiceFeature... features) {
        return getPort((EndpointReference)null,portInterface,features);
    }

    public <T> T getPort(Class<T> portInterface) throws WebServiceException {
        return getPort(portInterface, EMPTY_FEATURES);
    }

    public void addPort(QName portName, String bindingId, String endpointAddress) throws WebServiceException {
        if (!ports.containsKey(portName)) {
            BindingID bid = (bindingId == null) ? BindingID.SOAP11_HTTP : BindingID.parse(bindingId);
            ports.put(portName,
                    new PortInfo(this, EndpointAddress.create(endpointAddress), portName, bid));
        } else
            throw new WebServiceException("WSDLPort " + portName.toString() + " already exists can not create a port with the same name.");
    }

    QName addPort(EndpointReferenceInfo eprinfo) throws WebServiceException {
        QName portQName = eprinfo.pname;
        PortInfo portInfo = new PortInfo(this, EndpointAddress.create(eprinfo.uri), portQName, getPortModel(portQName).getBinding().getBindingId());
        if (!ports.containsKey(portQName)) {
            ports.put(portQName, portInfo);
        } //else
        //throw new WebServiceException("WSDLPort " + portName.toString() + " already exists can not create a port with the same name.");
        return portQName;
    }


    public <T> Dispatch<T> createDispatch(QName portName, Class<T>  aClass, Service.Mode mode) throws WebServiceException {
        return createDispatch(portName, aClass, mode, EMPTY_FEATURES);
    }

    private <T> Dispatch<T> createDispatch(QName portName, Class<T> aClass, Service.Mode mode, WebServiceFeature[] features, EndpointReference epr) {
        PortInfo port = safeGetPort(portName);
        BindingImpl binding = port.createBinding(features,null);
        Dispatch<T> dispatch = Stubs.createDispatch(portName, this, binding, aClass, mode, createPipeline(port, binding), WSEndpointReference.create(epr));
         serviceInterceptor.postCreateDispatch((WSBindingProvider) dispatch);
         return dispatch;

    }

    public <T> Dispatch<T> createDispatch(QName portName, Class<T> aClass, Service.Mode mode, WebServiceFeature... features) {
        return createDispatch(portName, aClass, mode, features, null);
    }

    public <T> Dispatch<T> createDispatch(EndpointReference endpointReference, Class<T> type, Service.Mode mode, WebServiceFeature... features) {
        //assert endpointReference != null;  check javadocs
        EndpointReferenceInfo eprInfo = new EndpointReferenceInfo(endpointReference);
        WSDLServiceImpl eprWsdlService = eprInfo.parseModel();
        wsdlService = (wsdlService == null)? eprWsdlService : wsdlService;
        QName portName = addPort(eprInfo);
        return createDispatch(portName, type, mode, features, endpointReference);
    }

    /**
     * Obtains {@link PortInfo} for the given name, with error check.
     */
    public
    @NotNull
    PortInfo safeGetPort(QName portName) {
        PortInfo port = ports.get(portName);
        if (port == null) {
            throw new WebServiceException(ClientMessages.INVALID_PORT_NAME(portName, buildNameList(ports.keySet())));
        }
        return port;
    }

    private StringBuilder buildNameList(Collection<QName> names) {
        StringBuilder sb = new StringBuilder();
        for (QName qn : names) {
            if (sb.length() > 0) sb.append(',');
            sb.append(qn);
        }
        return sb;
    }

    /**
     * Creates a new pipeline for the given port name.
     */
    private Tube createPipeline(PortInfo portInfo, WSBinding binding) {
        BindingID bindingId = portInfo.bindingId;

        TubelineAssembler assembler = TubelineAssemblerFactory.create(
                Thread.currentThread().getContextClassLoader(), bindingId);
        if (assembler == null)
            throw new WebServiceException("Unable to process bindingID=" + bindingId);    // TODO: i18n
        return assembler.createClient(
                new ClientTubeAssemblerContext(
                        portInfo.targetEndpoint,
                        portInfo.portModel,
                        this, binding, container));
    }

    public EndpointAddress getEndpointAddress(QName qName) {
        return ports.get(qName).targetEndpoint;
    }

    public Dispatch<Object> createDispatch(QName portName, JAXBContext jaxbContext, Service.Mode mode) throws WebServiceException {
        return createDispatch(portName, jaxbContext, mode, EMPTY_FEATURES);
    }

    private Dispatch<Object> createDispatch(QName portName, JAXBContext jaxbContext, Service.Mode mode, WebServiceFeature[] features, EndpointReference epr) {
        PortInfo port = safeGetPort(portName);
        BindingImpl binding = port.createBinding(features,null);
        Dispatch<Object> dispatch = Stubs.createJAXBDispatch(
                portName, this, binding, jaxbContext, mode,
                createPipeline(port, binding), WSEndpointReference.create(epr));
         serviceInterceptor.postCreateDispatch((WSBindingProvider)dispatch);
         return dispatch;
    }

    public Dispatch<Object> createDispatch(QName portName, JAXBContext jaxbContext, Service.Mode mode, WebServiceFeature... webServiceFeatures) {
        return createDispatch(portName, jaxbContext, mode, webServiceFeatures, null );
    }

    public Dispatch<Object> createDispatch(EndpointReference endpointReference, JAXBContext context, Service.Mode mode, WebServiceFeature... features) {
        //assert(endpointReference != null);   check javadocs
        EndpointReferenceInfo eprInfo = new EndpointReferenceInfo(endpointReference);
        WSDLServiceImpl eprWsdlService = eprInfo.parseModel();
        wsdlService = (wsdlService == null)? eprWsdlService : wsdlService;
        QName portName = addPort(eprInfo);
        return createDispatch(portName, context, mode, features, endpointReference );
    }

    public QName getServiceName() {
        return serviceName;
    }

    protected Class getServiceClass() {
        return serviceClass;
    }

    public Iterator<QName> getPorts() throws WebServiceException {
        // KK: the spec seems to be ambigous about whether
        // this returns ports that are dynamically added or not.
        if (ports.isEmpty())
            throw new WebServiceException("dii.service.no.wsdl.available");
        return ports.keySet().iterator();
    }

    public URL getWSDLDocumentLocation() {
        if(wsdlService==null)   return null;
        try {
            return new URL(wsdlService.getParent().getLocation().getSystemId());
        } catch (MalformedURLException e) {
            throw new AssertionError(e); // impossible
        }
    }

    private <T> T createEndpointIFBaseProxy(@Nullable WSEndpointReference epr,QName portName, Class<T> portInterface, WebServiceFeature[] webServiceFeatures) {
        //fail if service doesnt have WSDL
        if (wsdlService == null)
            throw new WebServiceException(ClientMessages.INVALID_SERVICE_NO_WSDL(serviceName));

        if (wsdlService.get(portName)==null) {
            throw new WebServiceException(
                ClientMessages.INVALID_PORT_NAME(portName,buildWsdlPortNames()));
        }

        SEIPortInfo eif = seiContext.get(portInterface);

        BindingImpl binding = eif.createBinding(webServiceFeatures,portInterface);
        SEIStub pis = new SEIStub(this, binding, eif.model, createPipeline(eif, binding), epr);

        T proxy = portInterface.cast(Proxy.newProxyInstance(portInterface.getClassLoader(),
                new Class[]{portInterface, WSBindingProvider.class, Closeable.class}, pis));
        if (serviceInterceptor != null) {
            serviceInterceptor.postCreateProxy((WSBindingProvider)proxy, portInterface);
        }
        return proxy;
    }

    /**
     * Lists up the port names in WSDL. For error diagnostics.
     */
    private StringBuilder buildWsdlPortNames() {
        Set<QName> wsdlPortNames = new HashSet<QName>();
        for (WSDLPortImpl port : wsdlService.getPorts())
            wsdlPortNames.add(port.getName());
        return buildNameList(wsdlPortNames);
    }

    /**
     * Obtains a {@link WSDLPortImpl} with error check.
     *
     * @return guaranteed to be non-null.
     */
    public @NotNull WSDLPortImpl getPortModel(QName portName) {
        WSDLPortImpl port = wsdlService.get(portName);
        if (port == null)
            throw new WebServiceException(
                ClientMessages.INVALID_PORT_NAME(portName,buildWsdlPortNames()));
        return port;
    }

    /**
     * Contributes to the construction of {@link WSServiceDelegate} by filling in
     * {@link SEIPortInfo} about a given SEI (linked from the {@link Service}-derived class.)
     */
    //todo: valid port in wsdl
    private void addSEI(QName portName, Class portInterface) throws WebServiceException {
        SEIPortInfo spi = seiContext.get(portInterface);
        if (spi != null) return;
        WSDLPortImpl wsdlPort = getPortModel(portName);
        RuntimeModeler modeler = new RuntimeModeler(portInterface, serviceName, wsdlPort);
        modeler.setPortName(portName);
        AbstractSEIModelImpl model = modeler.buildRuntimeModel();

        spi = new SEIPortInfo(this, portInterface, (SOAPSEIModel) model, wsdlPort);
        seiContext.put(spi.sei, spi);
        ports.put(spi.portName, spi);

    }

    public WSDLServiceImpl getWsdlService() {
        return wsdlService;
    }

    class EndpointReferenceInfo {
        private final @NotNull MemberSubmissionEndpointReference msepr;
        final String uri;
        final QName sname;
        final QName pname;

        EndpointReferenceInfo(EndpointReference epr) {
            msepr = EndpointReferenceUtil.transform(MemberSubmissionEndpointReference.class, epr);
            uri = msepr.addr.uri;
            sname = msepr.serviceName.name;
            pname = new QName(sname.getNamespaceURI(), msepr.serviceName.portName);
        }

        /**
         * Parses {@link com.sun.xml.ws.api.model.wsdl.WSDLService} from this EPR.
         */
        private WSDLServiceImpl parseModel() {
            WSDLModelImpl eprWsdlMdl;
            try {
                eprWsdlMdl = parseWSDL(new URL(uri), msepr.toWSDLSource());
            } catch (MalformedURLException e) {
                throw new WebServiceException(ClientMessages.INVALID_ADDRESS(uri));
            }

            validate(eprWsdlMdl);
            return eprWsdlMdl.getService(sname);
        }

        private void validate(WSDLModelImpl eprWsdlContext) {

            if (wsdlService != null) {
                //do we have the same wsdl
                if (!eprWsdlContext.getFirstServiceName().equals(serviceName))
                    throw new WebServiceException("EndpointReference WSDL ServiceName differs from Service Instance WSDL Service QName.\n" + " The two Service QNames must match");

                QName portName = pname;
                if (eprWsdlContext.getBinding(sname, portName) == null ||
                        wsdlService.get(portName) == null)
                    throw new WebServiceException("EndpointReference WSDL port name differs from Service Instance WSDL port QName.\n");

            } else {
                WSDLServiceImpl eprService = eprWsdlContext.getService(serviceName);
                if (eprService == null)
                    throw new WebServiceException(
                            ClientMessages.INVALID_SERVICE_NAME(serviceName,
                                    buildNameList(eprWsdlContext.getServices().keySet())));

                //if (!wsdlContext.contains(eprInfo.sname, new QName(eprInfo.sname.getNamespaceURI(), eprInfo.pname)))
                //    throw new WebServiceException("EndpointReference WSDL port name differs from Service Instance WSDL port QName.\n");

            }
        }
    }

     class DaemonThreadFactory implements ThreadFactory {
        public Thread newThread(Runnable r) {
            Thread daemonThread = new Thread(r);
            daemonThread.setDaemon(Boolean.TRUE);
            return daemonThread;
        }
    }

    private static final WebServiceFeature[] EMPTY_FEATURES = new WebServiceFeature[0];
}


