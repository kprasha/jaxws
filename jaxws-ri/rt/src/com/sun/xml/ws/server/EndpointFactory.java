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
import com.sun.istack.Nullable;
import com.sun.xml.ws.api.BindingID;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.addressing.MemberSubmissionAddressingFeature;
import com.sun.xml.ws.api.model.wsdl.WSDLBoundPortType;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.server.*;
import com.sun.xml.ws.api.wsdl.parser.WSDLParserExtension;
import com.sun.xml.ws.api.wsdl.writer.WSDLGeneratorExtension;
import com.sun.xml.ws.binding.BindingImpl;
import com.sun.xml.ws.binding.SOAPBindingImpl;
import com.sun.xml.ws.binding.BindingTypeImpl;
import com.sun.xml.ws.model.AbstractSEIModelImpl;
import com.sun.xml.ws.model.RuntimeModeler;
import com.sun.xml.ws.model.SOAPSEIModel;
import com.sun.xml.ws.model.wsdl.WSDLModelImpl;
import com.sun.xml.ws.model.wsdl.WSDLPortImpl;
import com.sun.xml.ws.resources.ServerMessages;
import com.sun.xml.ws.server.provider.ProviderEndpointModel;
import com.sun.xml.ws.server.provider.ProviderArgumentsBuilder;
import com.sun.xml.ws.server.provider.SyncProviderInvokerTube;
import com.sun.xml.ws.server.provider.AsyncProviderInvokerTube;
import com.sun.xml.ws.server.sei.SEIInvokerTube;
import com.sun.xml.ws.util.HandlerAnnotationInfo;
import com.sun.xml.ws.util.HandlerAnnotationProcessor;
import com.sun.xml.ws.util.ServiceConfigurationError;
import com.sun.xml.ws.util.ServiceFinder;
import com.sun.xml.ws.wsdl.parser.RuntimeWSDLParser;
import com.sun.xml.ws.wsdl.parser.XMLEntityResolver;
import com.sun.xml.ws.wsdl.parser.XMLEntityResolver.Parser;
import com.sun.xml.ws.wsdl.writer.WSDLGenerator;
import org.xml.sax.EntityResolver;
import org.xml.sax.SAXException;

import javax.jws.WebService;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.ws.*;
import javax.xml.ws.soap.SOAPBinding;
import javax.xml.ws.soap.AddressingFeature;
import javax.xml.ws.soap.MTOMFeature;
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
        @Nullable Collection<? extends SDDocumentSource> metadata, EntityResolver resolver, boolean isTransportSynchronous) {

        if(invoker ==null || implType ==null)
            throw new IllegalArgumentException();

        verifyImplementorClass(implType);

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

        {// error check
            String serviceNS = serviceName.getNamespaceURI();
            String portNS = portName.getNamespaceURI();
            if (!serviceNS.equals(portNS)) {
                throw new ServerRtException("wrong.tns.for.port",portNS, serviceNS);
            }
        }

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

        InvokerTube terminal;
        WSDLPort wsdlPort = null;
        AbstractSEIModelImpl seiModel = null;
        // create WSDL model
        if (primaryDoc != null) {
            wsdlPort = getWSDLPort(primaryDoc, docList, serviceName, portName);
        }
        WebServiceFeature[] wsfeatures = BindingTypeImpl.parseBindingType(implType);

        {// create terminal pipe that invokes the application
            if (implType.getAnnotation(WebServiceProvider.class)!=null) {
                ProviderEndpointModel model = new ProviderEndpointModel(implType, binding);
                ProviderArgumentsBuilder argsBuilder = ProviderArgumentsBuilder.create(model, binding);
                terminal = model.isAsync() ? new AsyncProviderInvokerTube(invoker, argsBuilder)
                        : new SyncProviderInvokerTube(invoker, argsBuilder);

                //Provider case:
                //         Enable Addressing from WSDL only if it has RespectBindingFeature enabled
                if (wsdlPort != null && BindingTypeImpl.isFeatureEnabled(RespectBindingFeature.ID, wsfeatures)) {
                    WebServiceFeature[] wsdlFeatures = extractExtraWSDLFeatures(wsdlPort,binding, true);
                    binding.setFeatures(wsdlFeatures);
                }
            } else {
                // Create runtime model for non Provider endpoints
                seiModel = createSEIModel(wsdlPort, implType, serviceName, portName, binding);
                if(binding instanceof SOAPBindingImpl){
                    //set portKnownHeaders on Binding, so that they can be used for MU processing
                    ((SOAPBindingImpl)binding).setPortKnownHeaders(
                            ((SOAPSEIModel)seiModel).getKnownHeaders());
                }
                terminal= new SEIInvokerTube(seiModel,invoker,binding);
                //SEI case:
                //         Enable Addressing from WSDL if it uses addressing
                if (wsdlPort != null) {
                    WebServiceFeature[] wsdlFeatures = extractExtraWSDLFeatures(wsdlPort,binding,false);
                    binding.setFeatures(wsdlFeatures);
                }
            }
            if (processHandlerAnnotation) {
                //Process @HandlerChain, if handler-chain is not set via Deployment Descriptor
                processHandlerAnnotation(binding, implType, serviceName, portName);
            }
            //Set Features in @BindingType
            binding.setFeatures(wsfeatures);
        }

        // Generate WSDL for SEI endpoints(not for Provider endpoints)
        if (primaryDoc == null) {
            if (implType.getAnnotation(WebServiceProvider.class)==null) {
                primaryDoc = generateWSDL(binding, seiModel, docList, container, implType);
                // create WSDL model
                wsdlPort = getWSDLPort(primaryDoc, docList, serviceName, portName);
            }
        }

        ServiceDefinitionImpl serviceDefiniton = (primaryDoc != null) ? new ServiceDefinitionImpl(docList, primaryDoc) : null;

        return new WSEndpointImpl<T>(serviceName, portName, binding,container,seiModel,wsdlPort,implType, serviceDefiniton,terminal, isTransportSynchronous);
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

    /**
     *
     * @param wsdlPort WSDLPort model
     * @param binding WSBinding for the corresponding port
     * @param honorWsdlRequired : If this is true add WSDL Feature only if wsd;Required=true
     *          In SEI case, it should be false
     *          In Provider case, it should be true
     * @return WebServiceFeature[] Extra features that are not already set on binding.
     *         i.e, if a feature is set already on binding through someother API
     *         the coresponding wsdlFeature is not set.
     */
    private static WebServiceFeature[] extractExtraWSDLFeatures(WSDLPort wsdlPort, WSBinding binding, boolean honorWsdlRequired) {
        List<WebServiceFeature> wsdlFeatures = null;
        if (wsdlPort != null) {
            wsdlFeatures = new ArrayList<WebServiceFeature>();

            AddressingFeature wsdlAddressingFeature = (AddressingFeature) wsdlPort.getFeature(AddressingFeature.ID);
            if (wsdlAddressingFeature == null) {
                //try MS Addressing Version
                wsdlAddressingFeature = (AddressingFeature) wsdlPort.getFeature(MemberSubmissionAddressingFeature.ID);
            }
            if ((wsdlAddressingFeature != null) &&
                    (binding.getFeature(wsdlAddressingFeature.getID()) == null)) {
                if ((honorWsdlRequired ? wsdlAddressingFeature.isRequired() : true))
                    wsdlFeatures.add(wsdlAddressingFeature);
            }

            WebServiceFeature wsdlMTOMFeature = wsdlPort.getFeature(MTOMFeature.ID);
            if ((wsdlMTOMFeature != null) &&
                   binding.getFeature(wsdlMTOMFeature.getID()) == null ) {
                    wsdlFeatures.add(wsdlMTOMFeature);
            }
            //these are the only features that jaxws pays attention portability wise.
        }
        return wsdlFeatures.toArray(new WebServiceFeature[] {});
    }
    /**
     * Verifies if the endpoint implementor class has @WebService or @WebServiceProvider
     * annotation
     *
     * @return
     *       true if it is a Provider or AsyncProvider endpoint
     *       false otherwise
     * @throws java.lang.IllegalArgumentException
     *      If it doesn't have any one of @WebService or @WebServiceProvider
     *      If it has both @WebService and @WebServiceProvider annotations
     */
    public static boolean verifyImplementorClass(Class<?> clz) {
        WebServiceProvider wsProvider = clz.getAnnotation(WebServiceProvider.class);
        WebService ws = clz.getAnnotation(WebService.class);
        if (wsProvider == null && ws == null) {
            throw new IllegalArgumentException(clz +" has neither @WebSerivce nor @WebServiceProvider annotation");
        }
        if (wsProvider != null && ws != null) {
            throw new IllegalArgumentException(clz +" has both @WebSerivce and @WebServiceProvider annotations");
        }
        if (wsProvider != null) {
            if (Provider.class.isAssignableFrom(clz) || AsyncProvider.class.isAssignableFrom(clz)) {
                return true;
            }
            throw new IllegalArgumentException(clz +" doesn't implement Provider or AsyncProvider interface");
        }
        return false;
    }


    private static AbstractSEIModelImpl createSEIModel(WSDLPort wsdlPort,
        Class<?> implType, @NotNull QName serviceName, @NotNull QName portName, WSBinding binding) {

        RuntimeModeler rap;
        // Create runtime model for non Provider endpoints

        // wsdlPort will be null, means we will generate WSDL. Hence no need to apply
        // bindings or need to look in the WSDL
        if(wsdlPort == null){
            rap = new RuntimeModeler(implType,serviceName, binding.getBindingId());
        } else {
            applyEffectiveMtomSetting(wsdlPort.getBinding(), binding);
            //now we got the Binding so lets build the model
            rap = new RuntimeModeler(implType, serviceName, (WSDLPortImpl)wsdlPort);
        }
        rap.setPortName(portName);
        return rap.buildRuntimeModel();
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
     * If service name is not already set via DD or programmatically, it uses
     * annotations {@link WebServiceProvider}, {@link WebService} on implementorClass to get PortName.
     *
     * @return non-null service name
     */
    public static @NotNull QName getDefaultServiceName(Class<?> implType) {
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
     * annotations on implementorClass to get PortName.
     *
     * @return non-null port name
     */
    public static @NotNull QName getDefaultPortName(QName serviceName, Class<?> implType) {
        QName portName;
        WebServiceProvider wsProvider = implType.getAnnotation(WebServiceProvider.class);
        if (wsProvider!=null) {
            String tns = wsProvider.targetNamespace();
            String local = wsProvider.portName();
            portName = new QName(tns, local);
        } else {
            portName = RuntimeModeler.getPortName(implType, serviceName.getNamespaceURI());
        }
        assert portName != null;
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
    private static SDDocumentImpl generateWSDL(WSBinding binding, AbstractSEIModelImpl seiModel, List<SDDocumentImpl> docs,
                                               Container container, Class implType) {
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
        WSDLGenerator wsdlGen = new WSDLGenerator(seiModel, wsdlResolver, binding, container, implType,
                ServiceFinder.find(WSDLGeneratorExtension.class).toArray());
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

    /**
     * Parses the primary WSDL and returns the {@link WSDLPort} for the given service and port names
     *
     * @param primaryWsdl Primary WSDL
     * @param metadata it may contain imported WSDL and schema documents
     * @param serviceName service name in wsdl
     * @param portName port name in WSDL
     * @return non-null wsdl port object
     */
    private static @NotNull WSDLPort getWSDLPort(SDDocumentSource primaryWsdl, List<? extends SDDocumentSource> metadata,
            @NotNull QName serviceName, @NotNull QName portName) {
        URL wsdlUrl = primaryWsdl.getSystemId();
        try {
            // TODO: delegate to another entity resolver
            WSDLModelImpl wsdlDoc = RuntimeWSDLParser.parse(
                new Parser(primaryWsdl), new EntityResolverImpl(metadata),
                    false, ServiceFinder.find(WSDLParserExtension.class).toArray());
            WSDLPort wsdlPort = wsdlDoc.getService(serviceName).get(portName);
            if (wsdlPort == null) {
                throw new ServerRtException("runtime.parser.wsdl.incorrectserviceport", serviceName, portName, wsdlUrl);
            }
            return wsdlPort;
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
