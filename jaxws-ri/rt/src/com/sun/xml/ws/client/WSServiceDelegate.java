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
import com.sun.xml.ws.Closeable;
import com.sun.xml.ws.addressing.EndpointReferenceUtil;
import com.sun.xml.ws.api.BindingID;
import com.sun.xml.ws.api.EndpointAddress;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.WSService;
import com.sun.xml.ws.api.addressing.MemberSubmissionEndpointReference;
import com.sun.xml.ws.api.client.ContainerResolver;
import com.sun.xml.ws.api.model.wsdl.WSDLModel;
import com.sun.xml.ws.api.pipe.ClientTubeAssemblerContext;
import com.sun.xml.ws.api.pipe.Stubs;
import com.sun.xml.ws.api.pipe.Tube;
import com.sun.xml.ws.api.pipe.TubelineAssembler;
import com.sun.xml.ws.api.pipe.TubelineAssemblerFactory;
import com.sun.xml.ws.api.server.Container;
import com.sun.xml.ws.api.wsdl.parser.WSDLParserExtension;
import com.sun.xml.ws.binding.BindingImpl;
import com.sun.xml.ws.client.sei.SEIStub;
import com.sun.xml.ws.handler.PortInfoImpl;
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
import com.sun.xml.ws.wsdl.parser.WSDLConstants;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.jws.HandlerChain;
import javax.xml.bind.JAXBContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Dispatch;
import javax.xml.ws.EndpointReference;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.HandlerResolver;
import javax.xml.ws.soap.SOAPBinding;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
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

    private HandlerResolver handlerResolver;

    private final Class<? extends Service> serviceClass;

    /**
     * Name of the service for which this {@link WSServiceDelegate} is created for.
     * Always non-null.
     */
    private final QName serviceName;

    /**
     * Information about SEI, keyed by their interface type.
     */
    private final Map<Class, SEIPortInfo> seiContext = new HashMap<Class, SEIPortInfo>();

    private final HashMap<QName, Set<String>> rolesMap = new HashMap<QName, Set<String>>();

    private Executor executor;

    /**
     * The WSDL service that this {@link Service} object represents.
     * <p>
     * This field is null iff no WSDL is given to {@link Service}.
     * TODO: is this really a supported scenario?
     */
    private WSDLServiceImpl wsdlService;

    private final Container container;


    public WSServiceDelegate(URL wsdlDocumentLocation, QName serviceName, final Class<? extends Service> serviceClass) {
        //we cant create a Service without serviceName
        if (serviceName == null)
            throw new WebServiceException(ClientMessages.INVALID_SERVICE_NAME_NULL(serviceName));
        this.serviceName = serviceName;
        this.serviceClass = serviceClass;
        this.container = ContainerResolver.getInstance().getContainer();

        if (wsdlDocumentLocation != null) {
            parseWSDL(wsdlDocumentLocation);
            populatePorts();
        }

        if (serviceClass != Service.class) {
            /*
        if (serviceClass != Service.class) {
            SCAnnotations serviceCAnnotations = new SCAnnotations(serviceClass);

            if(wsdlDocumentLocation==null)
                wsdlDocumentLocation = serviceCAnnotations.wsdlLocation;
            for (Class clazz : serviceCAnnotations.classes)
                addSEI(clazz);
        } else {
            if(wsdlDocumentLocation!=null)
                parseWSDL(wsdlDocumentLocation);
        }
        */
            //if @HandlerChain present, set HandlerResolver on service context
            HandlerChain handlerChain =
                    AccessController.doPrivileged(new PrivilegedAction<HandlerChain>() {
                        public HandlerChain run() {
                            return serviceClass.getAnnotation(HandlerChain.class);
                        }
                    });
            if (handlerChain != null) {
                HandlerResolverImpl hresolver = new HandlerResolverImpl(this);
                setHandlerResolver(hresolver);
            }
        }

    }

    /**
     * Parses the WSDL and builds {@link WSDLModel}.
     * <p>
     * TODO: the only reason this method isn't a part of the constructor is because
     * the code was written such a way that {@link #getPort(Class)} can inject a WSDL
     * into a {@link Service} that was created without one. Is it really a valid scenario?
     */
    private void parseWSDL(URL wsdlDocumentLocation) {
        if (wsdlDocumentLocation == null)
            throw new WebServiceException("No WSDL location Information present, error");

        WSDLModelImpl model = parseWSDL(wsdlDocumentLocation,null);
        wsdlService = model.getService(serviceName);
        if (wsdlService == null)
            throw new WebServiceException(
                    ClientMessages.INVALID_SERVICE_NAME(serviceName,
                            buildNameList(model.getServices().keySet())));
    }

    /**
     * Parses the WSDL and builds {@link WSDLModel}.
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

    // TODO: this method probably belong to EndpointReferenceInfo.
    private void validateEPR(WSDLModelImpl eprWsdlContext, EndpointReferenceInfo eprInfo) {

        if (wsdlService != null) {
            //do we have the same wsdl
            if (!eprWsdlContext.getFirstServiceName().equals(serviceName))
                throw new WebServiceException("EndpointReference WSDL ServiceName differs from Service Instance WSDL Service QName.\n" + " The two Service QNames must match");

            QName portName = eprInfo.pname;
            if (eprWsdlContext.getBinding(eprInfo.sname,portName)==null ||
                wsdlService.get(portName)==null)
                throw new WebServiceException("EndpointReference WSDL port name differs from Service Instance WSDL port QName.\n");

        } else {
            wsdlService = eprWsdlContext.getService(serviceName);
            if (wsdlService == null)
                throw new WebServiceException(
                        ClientMessages.INVALID_SERVICE_NAME(serviceName,
                                buildNameList(eprWsdlContext.getServices().keySet())));

            //if (!wsdlContext.contains(eprInfo.sname, new QName(eprInfo.sname.getNamespaceURI(), eprInfo.pname)))
            //    throw new WebServiceException("EndpointReference WSDL port name differs from Service Instance WSDL port QName.\n");

        }
    }

    private void populatePorts() {
        if (wsdlService != null) {

            /*
            //is this case needed as serviceName should not be null

            if (serviceName == null) {
                serviceName = wsdlContext.getFirstServiceName();
            }
            */
            // fill in statically known ports
            for (WSDLPortImpl port : wsdlService.getPorts())
                ports.put(port.getName(), new PortInfo(this, port));
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
        return getPort(portName, portInterface, (WebServiceFeature[]) null);
    }

    //milestone 2
    public <T> T getPort(QName portName, Class<T> portInterface, WebServiceFeature... webServiceFeatures) {
        if (portName == null || portInterface == null)
            throw new IllegalArgumentException();
        addSEI(portName, portInterface);
        return createEndpointIFBaseProxy(portName, portInterface, webServiceFeatures);
    }


    public <T> T getPort(EndpointReference endpointReference, Class<T> serviceEndpointInterface, WebServiceFeature... features) {
        throw new UnsupportedOperationException();
    }

    //milestone 2
    public <T> T getPort(Class<T> portInterface, WebServiceFeature... webServiceFeatures) {
        //get the first port corresponding to the SEI
        QName portTypeName = RuntimeModeler.getPortTypeName(portInterface);
        QName portName = wsdlService.getMatchingPort(portTypeName).getName();
        return getPort(portName, portInterface, webServiceFeatures);
    }

    public <T> T getPort(Class<T> portInterface) throws WebServiceException {
        return getPort(portInterface, (WebServiceFeature[]) null);
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
        return createDispatch(portName, aClass, mode, (WebServiceFeature[]) null);
    }

    //milestone 2
    public <T> Dispatch<T> createDispatch(QName portName, Class<T> aClass, Service.Mode mode, WebServiceFeature... webServiceFeatures) {
        PortInfo port = safeGetPort(portName);
        BindingImpl binding = port.createBinding(webServiceFeatures);
        return Stubs.createDispatch(portName, this, binding, aClass, mode, createPipeline(port, binding));
    }

    public <T> Dispatch<T> createDispatch(EndpointReference endpointReference, Class<T> type, Service.Mode mode, WebServiceFeature... features) {
        //assert endpointReference != null;  check javadocs
        EndpointReferenceInfo eprInfo = new EndpointReferenceInfo(endpointReference);
        eprInfo.parseModel();
        QName portName = addPort(eprInfo);
        return createDispatch(portName, type, mode, features);

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
        return createDispatch(portName, jaxbContext, mode, (WebServiceFeature[]) null);
    }

    //milestone 2.
    public Dispatch<Object> createDispatch(QName portName, JAXBContext jaxbContext, Service.Mode mode, WebServiceFeature... webServiceFeatures) {
        PortInfo port = safeGetPort(portName);
        BindingImpl binding = port.createBinding(webServiceFeatures);
        return Stubs.createJAXBDispatch(portName, this, binding, jaxbContext, mode,
                createPipeline(port, binding));
    }

    public Dispatch<Object> createDispatch(EndpointReference endpointReference, JAXBContext context, Service.Mode mode, WebServiceFeature... features) {
        //assert(endpointReference != null);   check javadocs
        EndpointReferenceInfo eprInfo = new EndpointReferenceInfo(endpointReference);
        eprInfo.parseModel();
        QName portName = addPort(eprInfo);
        return createDispatch(portName, context, mode, features);
    }

    public QName getServiceName() {
        return serviceName;
    }

    protected Class getServiceClass() {
        return serviceClass;
    }

    protected Set<String> getRoles(QName portName) {
        return rolesMap.get(portName);
    }

    protected void setRoles(QName portName, Set<String> roles) {
        rolesMap.put(portName, roles);
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

    private <T> T createEndpointIFBaseProxy(QName portName, Class<T> portInterface, WebServiceFeature[] webServiceFeatures) {
        //fail if service doesnt have WSDL
        if (wsdlService == null)
            throw new WebServiceException(ClientMessages.INVALID_SERVICE_NO_WSDL(serviceName));

        if (wsdlService.get(portName)==null) {
            throw new WebServiceException("WSDLPort " + portName + "is not found in service " + serviceName);
        }

        SEIPortInfo eif = seiContext.get(portInterface);

        BindingImpl binding = eif.createBinding(webServiceFeatures);
        SEIStub pis = new SEIStub(this, binding, eif.model, createPipeline(eif, binding), null);

        return portInterface.cast(Proxy.newProxyInstance(portInterface.getClassLoader(),
                new Class[]{portInterface, BindingProvider.class, Closeable.class}, pis));
    }

    /**
     * Determines the binding of the given port.
     */
    protected BindingImpl createBinding(QName portName, BindingID bindingId) {
        //take out?
        return createBinding(portName, bindingId, (WebServiceFeature[]) null);
    }


    /**
     * Determines the binding of the given port.
     */
    protected BindingImpl createBinding(QName portName, BindingID bindingId, WebServiceFeature... webServiceFeatures) {

        // get handler chain
        List<Handler> handlerChain;
        if (handlerResolver != null) {
            javax.xml.ws.handler.PortInfo portInfo = new PortInfoImpl(bindingId, portName, serviceName);
            handlerChain = handlerResolver.getHandlerChain(portInfo);
        } else {
            handlerChain = new ArrayList<Handler>();
        }

        // create binding
        BindingImpl bindingImpl = BindingImpl.create(bindingId, webServiceFeatures);
        PortInfo portInfo = ports.get(portName);
        if (portInfo.portModel != null && portInfo.portModel.getBinding().isMTOMEnabled()) {
            bindingImpl.setMTOMEnabled(true);
        }
        if (bindingImpl instanceof SOAPBinding) {
            Set<String> roles = rolesMap.get(portName);
            if (roles != null) {
                ((SOAPBinding) bindingImpl).setRoles(roles);
            }
        }

        bindingImpl.setHandlerChain(handlerChain);

        return bindingImpl;
    }


    /**
     * Obtains a {@link WSDLPortImpl} with error check.
     *
     * @return guaranteed to be non-null.
     */

    public WSDLPortImpl getPortModel(QName portName) {
        WSDLPortImpl port = wsdlService.get(portName);
        if (port == null)
            throw new WebServiceException("Port \"" + portName + "\" not found in service \"" + serviceName + "\"");
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
        // TODO: error check against wsdlPort==null
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
            if (epr.getClass().isAssignableFrom(MemberSubmissionEndpointReference.class)) {
                msepr = (MemberSubmissionEndpointReference) epr;
            } else {
                msepr = EndpointReferenceUtil.transform(MemberSubmissionEndpointReference.class, epr);
            }

            uri = msepr.addr.uri;
            sname = msepr.serviceName.name;
            pname = new QName(sname.getNamespaceURI(), msepr.serviceName.portName);
        }

        private Source createSource() {
            // get WSDL from WPR inline or imported
            Element wsdlElement = null;
            List<Element> elementz = msepr.elements;
            for (Element elem : elementz) {
                if (elem.getNamespaceURI().equals(WSDLConstants.NS_WSDL) &&
                        elem.getLocalName().equals(WSDLConstants.QNAME_DEFINITIONS.getLocalPart())) {
                    wsdlElement = elem;
                }
            }

            return new DOMSource(wsdlElement);
        }

        /**
         * Parses {@link WSDLModel} from this EPR.
         */
        private WSDLModel parseModel() {
            WSDLModelImpl eprWsdlCtx;
            try {
                eprWsdlCtx = parseWSDL(new URL(uri), createSource());
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }

            validateEPR(eprWsdlCtx,this);
            return eprWsdlCtx;
        }
    }

     class DaemonThreadFactory implements ThreadFactory {
        public Thread newThread(Runnable r) {
            Thread daemonThread = new Thread(r);
            daemonThread.setDaemon(Boolean.TRUE);
            return daemonThread;
        }
    }
}


