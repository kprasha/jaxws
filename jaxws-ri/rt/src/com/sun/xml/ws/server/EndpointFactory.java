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

package com.sun.xml.ws.server;

import com.sun.istack.NotNull;
import com.sun.xml.ws.api.BindingID;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.model.wsdl.WSDLService;
import com.sun.xml.ws.api.model.wsdl.WSDLBoundPortType;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.server.InstanceResolver;
import com.sun.xml.ws.api.server.SDDocument;
import com.sun.xml.ws.api.server.SDDocumentSource;
import com.sun.xml.ws.api.server.WSEndpoint;
import com.sun.xml.ws.api.wsdl.parser.WSDLParserExtension;
import com.sun.xml.ws.api.wsdl.writer.WSDLGeneratorExtension;
import com.sun.xml.ws.binding.BindingImpl;
import com.sun.xml.ws.binding.SOAPBindingImpl;
import com.sun.xml.ws.model.AbstractSEIModelImpl;
import com.sun.xml.ws.model.RuntimeModeler;
import com.sun.xml.ws.model.SOAPSEIModel;
import com.sun.xml.ws.model.wsdl.WSDLModelImpl;
import com.sun.xml.ws.model.wsdl.WSDLPortImpl;
import com.sun.xml.ws.resources.ServerMessages;
import com.sun.xml.ws.server.provider.ProviderEndpointModel;
import com.sun.xml.ws.server.provider.SOAPProviderInvokerPipe;
import com.sun.xml.ws.server.provider.XMLProviderInvokerPipe;
import com.sun.xml.ws.server.sei.SEIInvokerPipe;
import com.sun.xml.ws.api.server.Container;
import com.sun.xml.ws.api.server.Invoker;
import com.sun.xml.ws.util.HandlerAnnotationInfo;
import com.sun.xml.ws.util.HandlerAnnotationProcessor;
import com.sun.xml.ws.util.ServiceConfigurationError;
import com.sun.xml.ws.util.ServiceFinder;
import com.sun.xml.ws.wsdl.parser.RuntimeWSDLParser;
import com.sun.xml.ws.wsdl.parser.XMLEntityResolver;
import com.sun.xml.ws.wsdl.parser.XMLEntityResolver.Parser;
import com.sun.xml.ws.wsdl.writer.WSDLGenerator;
import com.sun.istack.Nullable;
import javax.jws.WebService;
import javax.xml.ws.WebServiceException;
import org.xml.sax.EntityResolver;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.ws.Provider;
import javax.xml.ws.WebServiceProvider;
import javax.xml.ws.soap.SOAPBinding;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Entry point to the JAX-WS RI server-side runtime.
 *
 * @author Kohsuke Kawaguchi
 * @author Jitendra Kotamraju
 */
public class EndpointFactory {

    /**
     * Implements {@link WSEndpoint#create}.
     *
     * No need to take WebServiceContext implementation. When InvokerPipe is
     * instantiated, it calls InstanceResolver to set up a WebServiceContext.
     * We shall only take delegate to getUserPrincipal and isUserInRole from adapter.
     *
     * <p>
     * Nobody else should be calling this method.
     */
    public static <T> WSEndpoint<T> createEndpoint(
        Class<T> implType, boolean processHandlerAnnotation, Invoker invoker, QName serviceName, QName portName,
        Container container, WSBinding binding,
        @Nullable SDDocumentSource primaryWsdl,
        @Nullable Collection<? extends SDDocumentSource> metadata, EntityResolver resolver) {

        if(invoker ==null || implType ==null)
            throw new IllegalArgumentException();

        List<SDDocumentSource> md = new ArrayList<SDDocumentSource>();
        if(metadata!=null)
            md.addAll(metadata);

        if(primaryWsdl!=null && !md.contains(primaryWsdl))
            md.add(primaryWsdl);

        if(container==null)
            container = Container.NONE;

        if(serviceName==null)
            serviceName = getDefaultServiceName(implType);

        if(portName==null)
            portName = getDefaultPortName(serviceName,implType);

        // setting a default binding
        if (binding == null)
            binding = BindingImpl.create(BindingID.parse(implType));

        if (primaryWsdl != null) {
            verifyPrimaryWSDL(primaryWsdl, serviceName);
        }

        QName portTypeName = null;
        if (implType.getAnnotation(WebServiceProvider.class)==null) {
            portTypeName = RuntimeModeler.getPortTypeName(implType);
        }

        // Categorises the documents as WSDL, Schema etc
        List<SDDocumentImpl> docList = categoriseMetadata(md, serviceName, portTypeName);
        // Finds the primary WSDL and makes sure that metadata doesn't have
        // two concrete or abstract WSDLs
        SDDocumentImpl primaryDoc = findPrimary(docList);

        Pipe terminal;
        WSDLPort wsdlPort = null;
        AbstractSEIModelImpl seiModel = null;

        {// create terminal pipe that invokes the application
            if (implType.getAnnotation(WebServiceProvider.class)!=null) {
                if (!Provider.class.isAssignableFrom(implType))
                    throw new ServerRtException("not.implement.provider",implType);

                ProviderEndpointModel model = new ProviderEndpointModel(implType.asSubclass(Provider.class), binding);
                if (binding instanceof SOAPBinding) {
                    SOAPVersion soapVersion = binding.getSOAPVersion();
                    terminal =  new SOAPProviderInvokerPipe(invoker, model, soapVersion);
                } else {
                    terminal =  new XMLProviderInvokerPipe(invoker, model);
                }
            } else {
                // Create runtime model for non Provider endpoints
                seiModel = createSEIModel(primaryDoc, md, implType, serviceName, portName, binding);
                wsdlPort = seiModel.getPort();

                if(binding instanceof SOAPBindingImpl){
                    //set portKnownHeaders on Binding, so that they can be used for MU processing
                    ((SOAPBindingImpl)binding).setPortKnownHeaders(
                            ((SOAPSEIModel)seiModel).getKnownHeaders());
                }
                terminal= new SEIInvokerPipe(seiModel,invoker,binding);
            }
            if (processHandlerAnnotation) {
                //Process @HandlerChain, if handler-chain is not set via Deployment Descriptor
                processHandlerAnnotation(binding, implType, serviceName, portName);
            }
        }

        // Generate WSDL for SEI endpoints(not for Provider endpoints)
        if (primaryDoc == null) {
            if (implType.getAnnotation(WebServiceProvider.class)==null) {
                primaryDoc = generateWSDL(binding, seiModel, docList, container);
            }
        }

        // create WSDL model
        if (wsdlPort == null && primaryDoc != null) {
            wsdlPort = getWSDLPort(primaryDoc, docList, implType, serviceName, portName);
        }

        {// error check
            String serviceNS = serviceName.getNamespaceURI();
            String portNS = portName.getNamespaceURI();
            if (!serviceNS.equals(portNS)) {
                throw new ServerRtException("wrong.tns.for.port",portNS, serviceNS);
            }
        }

        ServiceDefinitionImpl serviceDefiniton = (primaryDoc != null) ? new ServiceDefinitionImpl(docList, primaryDoc) : null;

        return new WSEndpointImpl<T>(binding,container,seiModel,wsdlPort,implType, serviceDefiniton,terminal);
    }

    private static <T> void processHandlerAnnotation(WSBinding binding, Class<T> implType, QName serviceName, QName portName) {
        HandlerAnnotationInfo chainInfo =
                HandlerAnnotationProcessor.buildHandlerInfo(
                        implType, serviceName, portName, binding);
        if (chainInfo != null) {
            binding.setHandlerChain(chainInfo.getHandlers());
            if (binding instanceof SOAPBinding) {
                ((SOAPBinding) binding).setRoles(chainInfo.getRoles());
            }
        }

    }


    private static AbstractSEIModelImpl createSEIModel(
        SDDocumentSource primaryWsdl, List<SDDocumentSource> metadata,
        Class<?> implType, QName serviceName, QName portName, WSBinding binding) {

        // Create runtime model for non Provider endpoints

        // wsdlURL will be null, means we will generate WSDL. Hence no need to apply
        // bindings or need to look in the WSDL
        if(primaryWsdl == null){
            RuntimeModeler rap = new RuntimeModeler(implType,serviceName, binding.getBindingId());
            if (portName != null) {
                rap.setPortName(portName);
            }
            return rap.buildRuntimeModel();
        }else {
            URL wsdlUrl = primaryWsdl.getSystemId();
            try {
                // TODO: delegate to another entity resolver
                WSDLModelImpl wsdlDoc = RuntimeWSDLParser.parse(
                    new Parser(primaryWsdl), new EntityResolverImpl(metadata),
                    ServiceFinder.find(WSDLParserExtension.class).toArray());
                WSDLPortImpl wsdlPort = null;
                if(serviceName == null)
                    serviceName = RuntimeModeler.getServiceName(implType);
                if(portName != null){
                    wsdlPort = wsdlDoc.getService(serviceName).get(portName);
                    if(wsdlPort == null)
                        throw new ServerRtException("runtime.parser.wsdl.incorrectserviceport", serviceName, portName, wsdlUrl);

                    // set the mtom enable setting from wsdl model (mtom policy assertion) if DD has not already set it. Also check
                    // conflicts.
                    applyEffectiveMtomSetting(wsdlPort.getBinding(), binding);
                }else{
                    WSDLService service = wsdlDoc.getService(serviceName);
                    if(service == null)
                        throw new ServerRtException("runtime.parser.wsdl.noservice", serviceName, wsdlUrl);

                    BindingID bindingId = binding.getBindingId();

                    // make sure there's one and only one port for the given binding
                    boolean hasPort = false;
                    for (WSDLPort p : service.getPorts()) {
                        if(p.getBinding().getBindingId().equals(bindingId)) {
                            if(hasPort)
                                throw new ServerRtException("runtime.parser.wsdl.multiplebinding", bindingId, serviceName, wsdlUrl);

                            //set the mtom enable setting from wsdl model (mtom policy assertion) if DD has not already set it. Also check
                            // conflicts.
                            applyEffectiveMtomSetting(p.getBinding(), binding);
                            hasPort = true;
                        }
                    }

                    if(!hasPort)
                        throw new ServerRtException("runtime.parser.wsdl.nobinding", bindingId, serviceName, wsdlUrl);
                }
                //now we got the Binding so lets build the model
                RuntimeModeler rap = new RuntimeModeler(implType, serviceName, wsdlPort);
                if (portName != null) {
                    rap.setPortName(portName);
                }
                return rap.buildRuntimeModel();
            } catch (IOException e) {
                throw new ServerRtException("runtime.parser.wsdl", wsdlUrl,e);
            } catch (XMLStreamException e) {
                throw new ServerRtException("runtime.saxparser.exception", e.getMessage(), e.getLocation(), e);
            } catch (SAXException e) {
                throw new ServerRtException("runtime.parser.wsdl", wsdlUrl,e);
            } catch (ServiceConfigurationError e) {
                throw new ServerRtException("runtime.parser.wsdl", wsdlUrl,e);
            }
        }
    }

    /**
     *Set the mtom enable setting from wsdl model (mtom policy assertion) on to @link WSBinding} if DD has
     * not already set it on BindingID. Also check conflicts.
     */

    private static void applyEffectiveMtomSetting(WSDLBoundPortType wsdlBinding, WSBinding binding){
        if(wsdlBinding.isMTOMEnabled()){
            BindingID bindingId = binding.getBindingId();
            if(bindingId.isMTOMEnabled() == null){
                binding.setMTOMEnabled(true);
            }else if (bindingId.isMTOMEnabled() != null && bindingId.isMTOMEnabled() == Boolean.FALSE){
                //TODO: i18N
                throw new ServerRtException("Deployment failed! Mtom policy assertion in WSDL is enabled whereas the deplyment descriptor setting wants to disable it!");
            }
        }
    }


    /**
     * Checks {@link WebServiceProvider} and determines the service name.
     */
    public static QName getDefaultServiceName(Class<?> implType) {
        QName serviceName;
        WebServiceProvider wsProvider = implType.getAnnotation(WebServiceProvider.class);
        if (wsProvider!=null) {
            String tns = wsProvider.targetNamespace();
            String local = wsProvider.serviceName();
            serviceName = new QName(tns, local);
        } else {
            serviceName = RuntimeModeler.getServiceName(implType);
        }
        assert serviceName != null;
        return serviceName;
    }

    /**
     * If portName is not already set via DD or programmatically, it uses
     * annotations on implementorClass to set PortName.
     */
    public static QName getDefaultPortName(QName serviceName, Class<?> implType) {
        QName portName;
        WebServiceProvider wsProvider = implType.getAnnotation(WebServiceProvider.class);
        if (wsProvider!=null) {
            String tns = wsProvider.targetNamespace();
            String local = wsProvider.portName();
            portName = new QName(tns, local);
        } else {
            portName = RuntimeModeler.getPortName(implType, serviceName.getNamespaceURI());
        }
        return portName;
    }
    
    /**
     * Returns the wsdl from @WebService, or @WebServiceProvider annotation using
     * wsdlLocation element.
     *
     * @param implType endpoint implementation class
     * @return wsdl if there is wsdlLocation, else null
     */
    public static @Nullable String getWsdlLocation(Class<?> implType) {
        String wsdl;
        WebService ws = implType.getAnnotation(WebService.class);
        if (ws != null) {
            wsdl = ws.wsdlLocation();
        } else {
            WebServiceProvider wsProvider = implType.getAnnotation(WebServiceProvider.class);
            assert wsProvider != null;
            wsdl = wsProvider.wsdlLocation();
        }
        if (wsdl.length() < 1) {
            wsdl = null;
        }
        return wsdl;
    }

    /**
     * Generates the WSDL and XML Schema for the endpoint if necessary
     * It generates WSDL only for SOAP1.1, and for XSOAP1.2 bindings
     */
    private static SDDocumentImpl generateWSDL(WSBinding binding, AbstractSEIModelImpl seiModel, List<SDDocumentImpl> docs, Container container) {
        BindingID bindingId = binding.getBindingId();
        if (!bindingId.canGenerateWSDL()) {
            throw new ServerRtException("can.not.generate.wsdl", bindingId);
        }

        if (bindingId.toString().equals(SOAPBindingImpl.X_SOAP12HTTP_BINDING)) {
            String msg = ServerMessages.GENERATE_NON_STANDARD_WSDL();
            logger.warning(msg);
        }

        // Generate WSDL and schema documents using runtime model
        WSDLGenResolver wsdlResolver = new WSDLGenResolver(docs,seiModel.getServiceQName(),seiModel.getPortTypeName());
        WSDLGenerator wsdlGen = new WSDLGenerator(seiModel, wsdlResolver, binding, container, ServiceFinder.find(WSDLGeneratorExtension.class).toArray());
        wsdlGen.doGeneration();
        return wsdlResolver.updateDocs();
    }

    /**
     * Builds {@link SDDocumentImpl} from {@link SDDocumentSource}.
     */
    private static List<SDDocumentImpl> categoriseMetadata(
        List<SDDocumentSource> src, QName serviceName, QName portTypeName) {

        List<SDDocumentImpl> r = new ArrayList<SDDocumentImpl>(src.size());
        for (SDDocumentSource doc : src) {
            r.add(SDDocumentImpl.create(doc,serviceName,portTypeName));
        }
        return r;
    }

    /**
     * Verifies whether the given primaryWsdl contains the given serviceName.
     * If the WSDL doesn't have the service, it throws an WebServiceException.
     */
    private static void verifyPrimaryWSDL(@NotNull SDDocumentSource primaryWsdl, @NotNull QName serviceName) {
        SDDocumentImpl primaryDoc = SDDocumentImpl.create(primaryWsdl,serviceName,null);
        if (!(primaryDoc instanceof SDDocument.WSDL)) {
            throw new WebServiceException("Not a primary WSDL="+primaryWsdl.getSystemId());
        }
        SDDocument.WSDL wsdlDoc = (SDDocument.WSDL)primaryDoc;
        if (!wsdlDoc.hasService()) {
            throw new WebServiceException("Not a primary WSDL="+primaryWsdl.getSystemId()+
                    " since it doesn't have Service "+serviceName);
        }
    }

    /**
     * Finds the primary WSDL document from the list of metadata documents. If
     * there are two metadata documents that qualify for primary, it throws an
     * exception. If there are two metadata documents that qualify for porttype,
     * it throws an exception.
     *
     * @return primay wsdl document, null if is not there in the docList
     *
     */
    private static @Nullable SDDocumentImpl findPrimary(@NotNull List<SDDocumentImpl> docList) {
        SDDocumentImpl primaryDoc = null;
        boolean foundConcrete = false;
        boolean foundAbstract = false;
        for(SDDocumentImpl doc : docList) {
            if (doc instanceof SDDocument.WSDL) {
                SDDocument.WSDL wsdlDoc = (SDDocument.WSDL)doc;
                if (wsdlDoc.hasService()) {
                    primaryDoc = doc;
                    if (foundConcrete) {
                        throw new ServerRtException("duplicate.primary.wsdl", doc.getSystemId() );
                    }
                    foundConcrete = true;
                }
                if (wsdlDoc.hasPortType()) {
                    if (foundAbstract) {
                        throw new ServerRtException("duplicate.abstract.wsdl", doc.getSystemId());
                    }
                    foundAbstract = true;
                }
            }
        }
        return primaryDoc;
    }
    
    private static WSDLPort getWSDLPort(SDDocumentSource primaryWsdl, List<? extends SDDocumentSource> metadata,
        Class<?> implType, QName serviceName, QName portName) {
        URL wsdlUrl = primaryWsdl.getSystemId();
        try {
            // TODO: delegate to another entity resolver
            WSDLModelImpl wsdlDoc = RuntimeWSDLParser.parse(
                new Parser(primaryWsdl), new EntityResolverImpl(metadata),
                ServiceFinder.find(WSDLParserExtension.class).toArray());
            if(serviceName == null)
                serviceName = RuntimeModeler.getServiceName(implType);
            if(portName != null) {
                return wsdlDoc.getService(serviceName).get(portName);
            }
        } catch (IOException e) {
            throw new ServerRtException("runtime.parser.wsdl", wsdlUrl,e);
        } catch (XMLStreamException e) {
            throw new ServerRtException("runtime.saxparser.exception", e.getMessage(), e.getLocation(), e);
        } catch (SAXException e) {
            throw new ServerRtException("runtime.parser.wsdl", wsdlUrl,e);
        } catch (ServiceConfigurationError e) {
            throw new ServerRtException("runtime.parser.wsdl", wsdlUrl,e);
        }
        return null;
    }

    /**
     * {@link XMLEntityResolver} that can resolve to {@link SDDocumentSource}s.
     */
    private static final class EntityResolverImpl implements XMLEntityResolver {
        private Map<String,SDDocumentSource> metadata = new HashMap<String,SDDocumentSource>();

        public EntityResolverImpl(List<? extends SDDocumentSource> metadata) {
            for (SDDocumentSource doc : metadata) {
                this.metadata.put(doc.getSystemId().toExternalForm(),doc);
            }
        }

        public Parser resolveEntity (String publicId, String systemId) throws IOException, XMLStreamException {
            if (systemId != null) {
                SDDocumentSource doc = metadata.get(systemId);
                if (doc != null)
                    return new Parser(doc);
            }
            return null;
        }

    }

    private static final Logger logger = Logger.getLogger(
        com.sun.xml.ws.util.Constants.LoggingDomain + ".server.endpoint");
}
