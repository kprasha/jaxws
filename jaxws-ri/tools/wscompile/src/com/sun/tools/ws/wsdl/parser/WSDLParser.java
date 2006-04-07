/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 * 
 * You can obtain a copy of the license at
 * https://jwsdp.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL
 * HEADER in each file and include the License file at
 * https://jwsdp.dev.java.net/CDDLv1.0.html  If applicable,
 * add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your
 * own identifying information: Portions Copyright [yyyy]
 * [name of copyright owner]
 */

package com.sun.tools.ws.wsdl.parser;

import com.sun.tools.ws.api.MetaDataResolver;
import com.sun.tools.ws.api.MetadataResolverFactory;
import com.sun.tools.ws.api.ServiceDescriptor;
import com.sun.tools.ws.api.wsdl.TWSDLExtensible;
import com.sun.tools.ws.api.wsdl.TWSDLExtensionHandler;
import com.sun.tools.ws.processor.config.WSDLModelInfo;
import com.sun.tools.ws.processor.util.ProcessorEnvironment;
import com.sun.tools.ws.util.xml.NullEntityResolver;
import com.sun.tools.ws.util.xml.XmlUtil;
import com.sun.tools.ws.wsdl.document.*;
import com.sun.tools.ws.wsdl.document.schema.SchemaConstants;
import com.sun.tools.ws.wsdl.document.schema.SchemaKinds;
import com.sun.tools.ws.wsdl.framework.Entity;
import com.sun.tools.ws.wsdl.framework.ParseException;
import com.sun.tools.ws.wsdl.framework.ParserListener;
import com.sun.tools.ws.wsdl.framework.TWSDLParserContextImpl;
import com.sun.tools.ws.wsdl.wxf.HTTPWxfClient;
import com.sun.tools.ws.resources.WsdlMessages;
import com.sun.xml.ws.util.DOMUtil;
import com.sun.xml.ws.util.JAXWSUtils;
import com.sun.xml.ws.util.ServiceFinder;
import static com.sun.xml.ws.util.xml.XmlUtil.DRACONIAN_ERROR_HANDLER;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * A parser for WSDL documents.
 *
 * @author WS Development Team
 */
public class WSDLParser {
    private WSDLModelInfo modelInfo;
    private EntityResolver entityResolver;
    //all the wsdl:import system Ids
    private final Set<String> imports = new HashSet<String>();
    //Map which holds wsdl Document(s) for a given SystemId
    private final Map<String, Element> wsdlDocuments = new HashMap<String, Element>();

    //inlined schema elements inside wsdl:type section
    private final List<Element> schemaElements;

    //wsdl extension handlers
    private final Map extensionHandlers;

    private final ProcessorEnvironment env;

    private String targetNamespaceURI;

    private ArrayList<ParserListener> listeners;

    private boolean useWxf = false;
    private HTTPWxfClient wxfClient;

    public WSDLParser(WSDLModelInfo modelInfo) {
        assert(modelInfo != null);
        this.extensionHandlers = new HashMap();
        this.schemaElements = new ArrayList<Element>();
        this.modelInfo = modelInfo;
        this.entityResolver = modelInfo.getEntityResolver();
        this.env = modelInfo.getConfiguration().getEnvironment();

        // register handlers for default extensions
        register(new SOAPExtensionHandler(extensionHandlers));
        register(new HTTPExtensionHandler(extensionHandlers));
        register(new MIMEExtensionHandler(extensionHandlers));
        register(new JAXWSBindingExtensionHandler(extensionHandlers));
        register(new SOAP12ExtensionHandler(extensionHandlers));

        for (TWSDLExtensionHandler te : ServiceFinder.find(TWSDLExtensionHandler.class).toArray()) {
            register(te);
        }

    }

    private void register(TWSDLExtensionHandler h) {
        extensionHandlers.put(h.getNamespaceURI(), h);
    }

    public void addParserListener(ParserListener l) {
        if (listeners == null) {
            listeners = new ArrayList<ParserListener>();
        }
        listeners.add(l);
    }

    private void removeParserListener(ParserListener l) {
        if (listeners == null) {
            return;
        }
        listeners.remove(l);
    }

    public boolean getUseWxf() {
        return useWxf;
    }

    public void setUseWxf(boolean b) {
        useWxf = b;
    }

    public WSDLDocument parse(){
        String location = modelInfo.getLocation();
        assert(location != null);
        WSDLDocument document = new WSDLDocument();
        InputSource source = null;
        String wsdlLoc = JAXWSUtils.absolutize(JAXWSUtils.getFileOrURLName(location));
        if(entityResolver != null){
            try {
                source = entityResolver.resolveEntity(null, wsdlLoc);
            } catch (SAXException e) {
                if (source.getSystemId() != null) {
                    throw new ParseException(
                        "parsing.saxExceptionWithSystemId",
                        source.getSystemId(),e);
                } else {
                    throw new ParseException("parsing.saxException",e);
                }
            } catch (IOException e) {
                if (source.getSystemId() != null) {
                    throw new ParseException(
                        "parsing.ioExceptionWithSystemId",
                        source.getSystemId(),e);
                } else {
                    throw new ParseException("parsing.ioException",e);
                }
            }
        }
        if(source == null){
            //default resolution
            source = new InputSource(wsdlLoc);
        }
        document.setSystemId(wsdlLoc);
        TWSDLParserContextImpl context = new TWSDLParserContextImpl(document, listeners);
        Definitions definitions = parseDefinitions(context, source, null);
        if(env.getErrorCount() > 0)
            return null;
        document.setDefinitions(definitions);
        return document;
    }

    protected Definitions parseDefinitions(TWSDLParserContextImpl context,
                                           InputSource source, String expectedTargetNamespaceURI) {
        context.pushWSDLLocation();
        context.setWSDLLocation(context.getDocument().getSystemId());
        String sysId = context.getDocument().getSystemId();
        Element root = null;

        if(sysId.endsWith("?wsdl")){
            try {
                root = buildDocumentFromWSDL(sysId, source, expectedTargetNamespaceURI);
            } catch (IOException e) {
                //lets try with MetadataResolverFactory
                root = getFromMetadataResolver(sysId);
                if(root == null){
                    env.error(WsdlMessages.localizablePARSING_UNABLE_TO_GET_METADATA(sysId));
                    return null;
                }
            }

        }else{
            root = getFromMetadataResolver(sysId);

            //if the metadata could not be resolved, try to the location and try to get the
            //metadata, it might be a wsdl on the filesystem
            if(root == null){
                try {
                    root = buildDocumentFromWSDL(sysId, source, expectedTargetNamespaceURI);
                } catch (IOException e) {
                    throw new ParseException(WsdlMessages.localizablePARSING_IO_EXCEPTION_WITH_SYSTEM_ID(sysId, e));
                }
            }
        }

        if(root == null)
            return null;

        //Internalizer.transform takes Set of jaxws:bindings elements, this is to allow multiple external
        //bindings to be transformed.
        new Internalizer().transform(modelInfo.getJAXWSBindings(), wsdlDocuments, modelInfo.getParent().getEnvironment());

        //print the wsdl
//        try{
//            dump(System.out);
//        }catch(IOException e){
//            e.printStackTrace();
//        }

        Definitions definitions = parseDefinitionsNoImport(context, root);
        processImports(context, source, definitions);
        context.popWSDLLocation();
        return definitions;
    }

    private Element getFromMetadataResolver(String systemId){
        //try MEX
        MetaDataResolver resolver = null;
        ServiceDescriptor serviceDescriptor = null;
        for(MetadataResolverFactory resolverFactory:ServiceFinder.find(MetadataResolverFactory.class)){
            resolver = resolverFactory.metadataResolver(entityResolver);
            try {
                serviceDescriptor = resolver.resolve(new URI(JAXWSUtils.getFileOrURLName(systemId)));
                //we got the ServiceDescriptor, now break
                if(serviceDescriptor != null)
                    break;
            } catch (URISyntaxException e) {
                throw new ParseException(e);
            }
        }

        if(serviceDescriptor != null){
            return parseMetadata(serviceDescriptor);
        }
        return null;
    }

    /**
     * Populates {@link #wsdlDocuments} and {@link #schemaElements} and returns root element
     */
    private Element parseMetadata(ServiceDescriptor serviceDescriptor){
        List<? extends Source> wsdls = serviceDescriptor.getWSDLs();
        List<? extends Source> schemas = serviceDescriptor.getSchemas();
        Element root = null;
        for(Source src:wsdls){
            if(src instanceof DOMSource){
                Node n = ((DOMSource) src).getNode();
                Element e = (n.getNodeType() == Node.ELEMENT_NODE)?(Element)n:DOMUtil.getFirstElementChild(n);
                if(root == null)
                    root = e;
                wsdlDocuments.put(src.getSystemId(), e);
            }
            //TODO:handle SAXSource
            //TODO:handler StreamSource
        }

        for(Source src:schemas){
            if(src instanceof DOMSource){
                Node n = ((DOMSource) src).getNode();
                Element e = (n.getNodeType() == Node.ELEMENT_NODE)?(Element)n:DOMUtil.getFirstElementChild(n);
                schemaElements.add(e);
            }
            //TODO:handle SAXSource
            //TODO:handler StreamSource
        }
        return root;
    }

    private Element buildDocumentFromWSDL(String systemId, InputSource source, String expectedTargetNamespaceURI) throws IOException {
        try {
            Document document = null;

            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            builderFactory.setNamespaceAware(true);
            builderFactory.setValidating(false);
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            builder.setErrorHandler(DRACONIAN_ERROR_HANDLER);
            if(entityResolver != null)
                builder.setEntityResolver(entityResolver);
            else
                builder.setEntityResolver(new NullEntityResolver());

            document = builder.parse(source);
            Element e = document.getDocumentElement();
            if(!Util.isTagName(e, WSDLConstants.QNAME_DEFINITIONS)){
                env.error(WsdlMessages.localizablePARSING_INVALID_TAG_NS(e.getTagName(),
                        e.getNamespaceURI(),
                        WSDLConstants.QNAME_DEFINITIONS.getLocalPart(),
                        WSDLConstants.QNAME_DEFINITIONS.getNamespaceURI(), systemId));
                return null;
            }
            wsdlDocuments.put(systemId, e);
            processWSDLDocument(e, expectedTargetNamespaceURI, source);
            return e;
        } catch (SAXException e) {
            throw new ParseException(WsdlMessages.localizablePARSING_SAX_EXCEPTION_WITH_SYSTEM_ID(source.getSystemId(), e));
        } catch (ParserConfigurationException e) {
            throw new ParseException(WsdlMessages.localizablePARSING_PARSER_CONFIG_EXCEPTION(e));
        } catch (FactoryConfigurationError e) {
            throw new ParseException(WsdlMessages.localizablePARSING_FACTORY_CONFIG_EXCEPTION(e));
        }
    }

    private void processWSDLDocument(Element e, String expectedTargetNamespaceURI, InputSource source) throws IOException, SAXException {
        String _targetNamespaceURI =
            XmlUtil.getAttributeOrNull(e, Constants.ATTR_TARGET_NAMESPACE);

        if (expectedTargetNamespaceURI != null
            && !expectedTargetNamespaceURI.equals(_targetNamespaceURI)){
            //TODO: throw an exception???
        }

        for (Iterator iter = XmlUtil.getAllChildren(e); iter.hasNext();) {
            Element e2 = Util.nextElement(iter);
            if (e2 == null)
                break;

            //check to see if it has imports
            if (XmlUtil.matchesTagNS(e2, WSDLConstants.QNAME_IMPORT)){
                String namespace = Util.getRequiredAttribute(e2, Constants.ATTR_NAMESPACE);
                String location = Util.getRequiredAttribute(e2, Constants.ATTR_LOCATION);
                location = getAdjustedLocation(source, location);
                if(location != null && !location.equals("")){
                    if(!imports.contains(location)){
                        imports.add(location);
                        InputSource impSource = null;
                        if(entityResolver != null){
                            impSource = entityResolver.resolveEntity(null, location);
                        }

                        if(impSource==null)
                            impSource = new InputSource(location);  // default resolution{

                        buildDocumentFromWSDL(location, impSource, namespace);
                    }
                }
            }
        }
    }

    private String getAdjustedLocation(InputSource source, String location) {
        return source.getSystemId() == null
            ? location
            : Util.processSystemIdWithBase(
                source.getSystemId(),
                location);
    }

    /**
     * Dumps the contents of the forest to the specified stream.
     *
     * This is a debug method. As such, error handling is sloppy.
     */
    public void dump( OutputStream out ) throws IOException {
        try {
            // create identity transformer
            Transformer it = XmlUtil.newTransformer();

            for( Iterator itr=wsdlDocuments.entrySet().iterator(); itr.hasNext(); ) {
                Map.Entry e = (Map.Entry)itr.next();

                out.write( ("---<< "+e.getKey()+"\n").getBytes() );

                it.transform( new DOMSource((Document)e.getValue()), new StreamResult(out) );

                out.write( "\n\n\n".getBytes() );
            }
        } catch( TransformerException e ) {
            e.printStackTrace();
        }
    }

    private void processImports(TWSDLParserContextImpl context, InputSource source, Definitions definitions) {
        for(String location : imports){
            if (!context.getDocument().isImportedDocument(location)){
                Definitions importedDefinitions = parseDefinitionsNoImport(context, wsdlDocuments.get(location));
                if(importedDefinitions == null)
                    continue;
                context.getDocument().addImportedEntity(importedDefinitions);
                context.getDocument().addImportedDocument(location);
            }
        }
    }

    private Definitions parseDefinitionsNoImport(
        TWSDLParserContextImpl context,
        Element e) {
        context.push();
        context.registerNamespaces(e);

        Definitions definitions = new Definitions(context.getDocument());
        String name = XmlUtil.getAttributeOrNull(e, Constants.ATTR_NAME);
        definitions.setName(name);

        targetNamespaceURI =
            XmlUtil.getAttributeOrNull(e, Constants.ATTR_TARGET_NAMESPACE);

        definitions.setTargetNamespaceURI(targetNamespaceURI);

        boolean gotDocumentation = false;
        boolean gotTypes = false;

        for (Iterator iter = XmlUtil.getAllChildren(e); iter.hasNext();) {
            Element e2 = Util.nextElement(iter);
            if (e2 == null)
                break;

            if (XmlUtil.matchesTagNS(e2, WSDLConstants.QNAME_DOCUMENTATION)) {
                if (gotDocumentation) {
                    env.error(WsdlMessages.localizablePARSING_ONLY_ONE_DOCUMENTATION_ALLOWED(e.getLocalName()));
                    return null;
                }
                gotDocumentation = true;
                if(definitions.getDocumentation() == null)
                    definitions.setDocumentation(getDocumentationFor(e2));
            } else if (XmlUtil.matchesTagNS(e2, WSDLConstants.QNAME_TYPES)) {
                if (gotTypes) {
                    env.error(WsdlMessages.localizablePARSING_ONLY_ONE_TYPES_ALLOWED(Constants.TAG_DEFINITIONS));
                    return null;
                }
                //add all the wsdl:type elements to latter make a list of all the schema elements
                // that will be needed to create jaxb model
                addSchemaElements(e2);
            } else if (XmlUtil.matchesTagNS(e2, WSDLConstants.QNAME_MESSAGE)) {
                Message message = parseMessage(context, definitions, e2);
                definitions.add(message);
            } else if (
                XmlUtil.matchesTagNS(e2, WSDLConstants.QNAME_PORT_TYPE)) {
                PortType portType = parsePortType(context, definitions, e2);
                definitions.add(portType);
            } else if (XmlUtil.matchesTagNS(e2, WSDLConstants.QNAME_BINDING)) {
                Binding binding = parseBinding(context, definitions, e2);
                definitions.add(binding);
            } else if (XmlUtil.matchesTagNS(e2, WSDLConstants.QNAME_SERVICE)) {
                Service service = parseService(context, definitions, e2);
                definitions.add(service);
            } else if (XmlUtil.matchesTagNS(e2, WSDLConstants.QNAME_IMPORT)) {
                definitions.add(parseImport(context, definitions, e2));
            } else if (XmlUtil.matchesTagNS(e2, SchemaConstants.QNAME_IMPORT)) {
                env.warn(WsdlMessages.localizableWARNING_WSI_R_2003());
            } else {
                // possible extensibility element -- must live outside the WSDL namespace
                checkNotWsdlElement(e2);
                if (!handleExtension(context, definitions, e2)) {
                    checkNotWsdlRequired(e2);
                }
            }
        }

        context.pop();
        context.fireDoneParsingEntity(
            WSDLConstants.QNAME_DEFINITIONS,
            definitions);
        return definitions;
    }

    protected Message parseMessage(
        TWSDLParserContextImpl context,
        Definitions definitions,
        Element e) {
        context.push();
        context.registerNamespaces(e);
        Message message = new Message(definitions);
        String name = Util.getRequiredAttribute(e, Constants.ATTR_NAME);
        message.setName(name);

        boolean gotDocumentation = false;

        for (Iterator iter = XmlUtil.getAllChildren(e); iter.hasNext();) {
            Element e2 = Util.nextElement(iter);
            if (e2 == null)
                break;

            if (XmlUtil.matchesTagNS(e2, WSDLConstants.QNAME_DOCUMENTATION)) {
                if (gotDocumentation) {
                    Util.fail(
                        "parsing.onlyOneDocumentationAllowed",
                        e.getLocalName());
                }
                gotDocumentation = true;
                message.setDocumentation(getDocumentationFor(e2));
            } else if (XmlUtil.matchesTagNS(e2, WSDLConstants.QNAME_PART)) {
                MessagePart part = parseMessagePart(context, e2);
                message.add(part);
            } else {
                Util.fail(
                    "parsing.invalidElement",
                    e2.getTagName(),
                    e2.getNamespaceURI());
            }
        }

        context.pop();
        context.fireDoneParsingEntity(WSDLConstants.QNAME_MESSAGE, message);
        return message;
    }

    protected MessagePart parseMessagePart(TWSDLParserContextImpl context, Element e) {
        context.push();
        context.registerNamespaces(e);
        MessagePart part = new MessagePart();
        String partName = Util.getRequiredAttribute(e, Constants.ATTR_NAME);
        part.setName(partName);

        String elementAttr =
            XmlUtil.getAttributeOrNull(e, Constants.ATTR_ELEMENT);
        String typeAttr = XmlUtil.getAttributeOrNull(e, Constants.ATTR_TYPE);

        if (elementAttr != null) {
            if (typeAttr != null) {
                Util.fail("parsing.onlyOneOfElementOrTypeRequired", partName);
            }

            part.setDescriptor(context.translateQualifiedName(elementAttr));
            part.setDescriptorKind(SchemaKinds.XSD_ELEMENT);
        } else if (typeAttr != null) {
            part.setDescriptor(context.translateQualifiedName(typeAttr));
            part.setDescriptorKind(SchemaKinds.XSD_TYPE);
        } else {
            // XXX-NOTE - this is wrong; for extensibility purposes,
            // any attribute can be specified on a <part> element, so
            // we need to put an extensibility hook here
            Util.fail("parsing.elementOrTypeRequired", partName);
        }

        context.pop();
        context.fireDoneParsingEntity(WSDLConstants.QNAME_PART, part);
        return part;
    }

    protected PortType parsePortType(
        TWSDLParserContextImpl context,
        Definitions definitions,
        Element e) {
        context.push();
        context.registerNamespaces(e);
        PortType portType = new PortType(definitions);
        String name = Util.getRequiredAttribute(e, Constants.ATTR_NAME);
        portType.setName(name);

        boolean gotDocumentation = false;

        for (Iterator iter = XmlUtil.getAllChildren(e); iter.hasNext();) {
            Element e2 = Util.nextElement(iter);
            if (e2 == null)
                break;

            if (XmlUtil.matchesTagNS(e2, WSDLConstants.QNAME_DOCUMENTATION)) {
                if (gotDocumentation) {
                    Util.fail(
                        "parsing.onlyOneDocumentationAllowed",
                        e.getLocalName());
                }
                gotDocumentation = true;
                if(portType.getDocumentation() == null)
                    portType.setDocumentation(getDocumentationFor(e2));
            } else if (
                XmlUtil.matchesTagNS(e2, WSDLConstants.QNAME_OPERATION)) {
                Operation op = parsePortTypeOperation(context, e2);
                op.setParent(portType);
                portType.add(op);
            } else {
                // possible extensibility element -- must live outside the WSDL namespace
                checkNotWsdlElement(e2);
                if (!handleExtension(context, portType, e2)) {
                    checkNotWsdlRequired(e2);
                }
            }/*else {
                Util.fail(
                    "parsing.invalidElement",
                    e2.getTagName(),
                    e2.getNamespaceURI());
            }*/
        }

        context.pop();
        context.fireDoneParsingEntity(WSDLConstants.QNAME_PORT_TYPE, portType);
        return portType;
    }

    protected Operation parsePortTypeOperation(
        TWSDLParserContextImpl context,
        Element e) {
        context.push();
        context.registerNamespaces(e);

        Operation operation = new Operation();
        String name = Util.getRequiredAttribute(e, Constants.ATTR_NAME);
        operation.setName(name);
        String parameterOrderAttr =
            XmlUtil.getAttributeOrNull(e, Constants.ATTR_PARAMETER_ORDER);
        operation.setParameterOrder(parameterOrderAttr);

        boolean gotDocumentation = false;

        boolean gotInput = false;
        boolean gotOutput = false;
        boolean gotFault = false;
        boolean inputBeforeOutput = false;

        for (Iterator iter = XmlUtil.getAllChildren(e); iter.hasNext();) {
            Element e2 = Util.nextElement(iter);
            if (e2 == null)
                break;

            if (XmlUtil.matchesTagNS(e2, WSDLConstants.QNAME_DOCUMENTATION)) {
                if (gotDocumentation) {
                    Util.fail(
                        "parsing.onlyOneDocumentationAllowed",
                        e.getLocalName());
                }
                gotDocumentation = true;
                if(operation.getDocumentation() == null)
                    operation.setDocumentation(getDocumentationFor(e2));
            } else if (XmlUtil.matchesTagNS(e2, WSDLConstants.QNAME_INPUT)) {
                if (gotInput) {
                    Util.fail(
                        "parsing.tooManyElements",
                        new Object[] {
                            Constants.TAG_INPUT,
                            Constants.TAG_OPERATION,
                            name });
                }

                context.push();
                context.registerNamespaces(e2);
                Input input = new Input();
                input.setParent(operation);
                String messageAttr =
                    Util.getRequiredAttribute(e2, Constants.ATTR_MESSAGE);
                input.setMessage(context.translateQualifiedName(messageAttr));
                String nameAttr =
                    XmlUtil.getAttributeOrNull(e2, Constants.ATTR_NAME);
                input.setName(nameAttr);
                operation.setInput(input);
                gotInput = true;
                if (gotOutput) {
                    inputBeforeOutput = false;
                }

                // check for extensiblity attributes
                for (Iterator iter2 = XmlUtil.getAllAttributes(e2);
                     iter2.hasNext();
                ) {
                    Attr e3 = (Attr)iter2.next();
                    if (e3.getLocalName().equals(Constants.ATTR_MESSAGE) ||
                        e3.getLocalName().equals(Constants.ATTR_NAME))
                        continue;

                    // possible extensibility element -- must live outside the WSDL namespace
                    checkNotWsdlAttribute(e3);
                    if (!handleExtension(context, input, e3, e2)) {
                        // ignore the extensiblity attribute
                        // TODO throw a WARNING
                    }
                }

                // verify that there is at most one child element and it is a documentation element
                boolean gotDocumentation2 = false;
                for (Iterator iter2 = XmlUtil.getAllChildren(e2);
                     iter2.hasNext();
                    ) {
                    Element e3 = Util.nextElement(iter2);
                    if (e3 == null)
                        break;

                    if (XmlUtil
                        .matchesTagNS(e3, WSDLConstants.QNAME_DOCUMENTATION)) {
                        if (gotDocumentation2) {
                            Util.fail(
                                "parsing.onlyOneDocumentationAllowed",
                                e.getLocalName());
                        }
                        gotDocumentation2 = true;
                        input.setDocumentation(getDocumentationFor(e3));
                    } else {
                        Util.fail(
                            "parsing.invalidElement",
                            e3.getTagName(),
                            e3.getNamespaceURI());
                    }
                }
                context.pop();
            } else if (XmlUtil.matchesTagNS(e2, WSDLConstants.QNAME_OUTPUT)) {
                if (gotOutput) {
                    Util.fail(
                        "parsing.tooManyElements",
                        new Object[] {
                            Constants.TAG_OUTPUT,
                            Constants.TAG_OPERATION,
                            name });
                }

                context.push();
                context.registerNamespaces(e2);
                Output output = new Output();
                output.setParent(operation);
                String messageAttr =
                    Util.getRequiredAttribute(e2, Constants.ATTR_MESSAGE);
                output.setMessage(context.translateQualifiedName(messageAttr));
                String nameAttr =
                    XmlUtil.getAttributeOrNull(e2, Constants.ATTR_NAME);
                output.setName(nameAttr);
                operation.setOutput(output);
                gotOutput = true;
                if (gotInput) {
                    inputBeforeOutput = true;
                }

                // check for extensiblity attributes
                for (Iterator iter2 = XmlUtil.getAllAttributes(e2);
                     iter2.hasNext();
                ) {
                    Attr e3 = (Attr)iter2.next();
                    if (e3.getLocalName().equals(Constants.ATTR_MESSAGE) ||
                        e3.getLocalName().equals(Constants.ATTR_NAME))
                        continue;

                    // possible extensibility element -- must live outside the WSDL namespace
                    checkNotWsdlAttribute(e3);
                    if (!handleExtension(context, output, e3, e2)) {
                        // ignore the extensiblity attribute
                        // TODO throw a WARNING
                    }
                }

                // verify that there is at most one child element and it is a documentation element
                boolean gotDocumentation2 = false;
                for (Iterator iter2 = XmlUtil.getAllChildren(e2);
                     iter2.hasNext();
                    ) {
                    Element e3 = Util.nextElement(iter2);
                    if (e3 == null)
                        break;

                    if (XmlUtil
                        .matchesTagNS(e3, WSDLConstants.QNAME_DOCUMENTATION)) {
                        if (gotDocumentation2) {
                            Util.fail(
                                "parsing.onlyOneDocumentationAllowed",
                                e.getLocalName());
                        }
                        gotDocumentation2 = true;
                        output.setDocumentation(getDocumentationFor(e3));
                    } else {
                        Util.fail(
                            "parsing.invalidElement",
                            e3.getTagName(),
                            e3.getNamespaceURI());
                    }
                }
                context.pop();
            } else if (XmlUtil.matchesTagNS(e2, WSDLConstants.QNAME_FAULT)) {
                context.push();
                context.registerNamespaces(e2);
                Fault fault = new Fault();
                fault.setParent(operation);
                String messageAttr =
                    Util.getRequiredAttribute(e2, Constants.ATTR_MESSAGE);
                fault.setMessage(context.translateQualifiedName(messageAttr));
                String nameAttr =
                    XmlUtil.getAttributeOrNull(e2, Constants.ATTR_NAME);
                fault.setName(nameAttr);
                operation.addFault(fault);
                gotFault = true;

                // check for extensiblity attributes
                for (Iterator iter2 = XmlUtil.getAllAttributes(e2);
                     iter2.hasNext();
                ) {
                    Attr e3 = (Attr)iter2.next();
                    if (e3.getLocalName().equals(Constants.ATTR_MESSAGE) ||
                        e3.getLocalName().equals(Constants.ATTR_NAME))
                        continue;

                    // possible extensibility element -- must live outside the WSDL namespace
                    checkNotWsdlAttribute(e3);
                    if (!handleExtension(context, fault, e3, e2)) {
                        // ignore the extensiblity attribute
                        // TODO throw a WARNING
                    }
                }

                // verify that there is at most one child element and it is a documentation element
                boolean gotDocumentation2 = false;
                for (Iterator iter2 = XmlUtil.getAllChildren(e2);
                     iter2.hasNext();
                    ) {
                    Element e3 = Util.nextElement(iter2);
                    if (e3 == null)
                        break;

                    if (XmlUtil
                        .matchesTagNS(e3, WSDLConstants.QNAME_DOCUMENTATION)) {
                        if (gotDocumentation2) {
                            Util.fail(
                                "parsing.onlyOneDocumentationAllowed",
                                e.getLocalName());
                        }
                        gotDocumentation2 = true;
                        if(fault.getDocumentation() == null)
                            fault.setDocumentation(getDocumentationFor(e3));
                    } else {
                        // possible extensibility element -- must live outside the WSDL namespace
                        checkNotWsdlElement(e3);
                        if (!handleExtension(context, fault, e3)) {
                            checkNotWsdlRequired(e3);
                        }
                    }/*else {
                        Util.fail(
                            "parsing.invalidElement",
                            e3.getTagName(),
                            e3.getNamespaceURI());
                    }*/
                }
                context.pop();
            } else {
                // possible extensibility element -- must live outside the WSDL namespace
                checkNotWsdlElement(e2);
                if (!handleExtension(context, operation, e2)) {
                    checkNotWsdlRequired(e2);
                }
            }/*else {
                Util.fail(
                    "parsing.invalidElement",
                    e2.getTagName(),
                    e2.getNamespaceURI());
            }*/
        }

        if (gotInput && !gotOutput && !gotFault) {
            operation.setStyle(OperationStyle.ONE_WAY);
        } else if (gotInput && gotOutput && inputBeforeOutput) {
            operation.setStyle(OperationStyle.REQUEST_RESPONSE);
        } else if (gotInput && gotOutput && !inputBeforeOutput) {
            operation.setStyle(OperationStyle.SOLICIT_RESPONSE);
        } else if (gotOutput && !gotInput && !gotFault) {
            operation.setStyle(OperationStyle.NOTIFICATION);
        } else {
            Util.fail("parsing.invalidOperationStyle", name);
        }

        context.pop();
        context.fireDoneParsingEntity(WSDLConstants.QNAME_OPERATION, operation);
        return operation;
    }

    protected Binding parseBinding(
        TWSDLParserContextImpl context,
        Definitions definitions,
        Element e) {
        context.push();
        context.registerNamespaces(e);
        Binding binding = new Binding(definitions);
        String name = Util.getRequiredAttribute(e, Constants.ATTR_NAME);
        binding.setName(name);
        String typeAttr = Util.getRequiredAttribute(e, Constants.ATTR_TYPE);
        binding.setPortType(context.translateQualifiedName(typeAttr));

        boolean gotDocumentation = false;

        for (Iterator iter = XmlUtil.getAllChildren(e); iter.hasNext();) {
            Element e2 = Util.nextElement(iter);
            if (e2 == null)
                break;

            if (XmlUtil.matchesTagNS(e2, WSDLConstants.QNAME_DOCUMENTATION)) {
                if (gotDocumentation) {
                    Util.fail(
                        "parsing.onlyOneDocumentationAllowed",
                        e.getLocalName());
                }
                gotDocumentation = true;
                binding.setDocumentation(getDocumentationFor(e2));
            } else if (
                XmlUtil.matchesTagNS(e2, WSDLConstants.QNAME_OPERATION)) {
                BindingOperation op = parseBindingOperation(context, e2);
                binding.add(op);
            } else {
                // possible extensibility element -- must live outside the WSDL namespace
                checkNotWsdlElement(e2);
                if (!handleExtension(context, binding, e2)) {
                    checkNotWsdlRequired(e2);
                }
            }
        }

        context.pop();
        context.fireDoneParsingEntity(WSDLConstants.QNAME_BINDING, binding);
        return binding;
    }

    protected BindingOperation parseBindingOperation(
        TWSDLParserContextImpl context,
        Element e) {
        context.push();
        context.registerNamespaces(e);
        BindingOperation operation = new BindingOperation();
        String name = Util.getRequiredAttribute(e, Constants.ATTR_NAME);
        operation.setName(name);

        boolean gotDocumentation = false;

        boolean gotInput = false;
        boolean gotOutput = false;
        boolean gotFault = false;
        boolean inputBeforeOutput = false;

        for (Iterator iter = XmlUtil.getAllChildren(e); iter.hasNext();) {
            Element e2 = Util.nextElement(iter);
            if (e2 == null)
                break;
            if (XmlUtil.matchesTagNS(e2, WSDLConstants.QNAME_DOCUMENTATION)) {
                if (gotDocumentation) {
                    Util.fail(
                        "parsing.onlyOneDocumentationAllowed",
                        e.getLocalName());
                }
                gotDocumentation = true;
                operation.setDocumentation(getDocumentationFor(e2));
            } else if (XmlUtil.matchesTagNS(e2, WSDLConstants.QNAME_INPUT)) {
                if (gotInput) {
                    Util.fail(
                        "parsing.tooManyElements",
                        new Object[] {
                            Constants.TAG_INPUT,
                            Constants.TAG_OPERATION,
                            name });
                }

                /* Here we check for the use scenario */
                Iterator itere2 = XmlUtil.getAllChildren(e2);
                Element ee = Util.nextElement(itere2);
                context.push();
                context.registerNamespaces(e2);
                BindingInput input = new BindingInput();
                String nameAttr =
                    XmlUtil.getAttributeOrNull(e2, Constants.ATTR_NAME);
                input.setName(nameAttr);
                operation.setInput(input);
                gotInput = true;
                if (gotOutput) {
                    inputBeforeOutput = false;
                }

                // verify that there is at most one child element and it is a documentation element
                boolean gotDocumentation2 = false;
                for (Iterator iter2 = XmlUtil.getAllChildren(e2);
                     iter2.hasNext();
                    ) {
                    Element e3 = Util.nextElement(iter2);
                    if (e3 == null)
                        break;

                    if (XmlUtil
                        .matchesTagNS(e3, WSDLConstants.QNAME_DOCUMENTATION)) {
                        if (gotDocumentation2) {
                            Util.fail(
                                "parsing.onlyOneDocumentationAllowed",
                                e.getLocalName());
                        }
                        gotDocumentation2 = true;
                        input.setDocumentation(getDocumentationFor(e3));
                    } else {
                        // possible extensibility element -- must live outside the WSDL namespace
                        checkNotWsdlElement(e3);
                        if (!handleExtension(context, input, e3)) {
                            checkNotWsdlRequired(e3);
                        }
                    }
                }
                context.pop();
            } else if (XmlUtil.matchesTagNS(e2, WSDLConstants.QNAME_OUTPUT)) {
                if (gotOutput) {
                    Util.fail(
                        "parsing.tooManyElements",
                        new Object[] {
                            Constants.TAG_OUTPUT,
                            Constants.TAG_OPERATION,
                            name });
                }

                context.push();
                context.registerNamespaces(e2);
                BindingOutput output = new BindingOutput();
                String nameAttr =
                    XmlUtil.getAttributeOrNull(e2, Constants.ATTR_NAME);
                output.setName(nameAttr);
                operation.setOutput(output);
                gotOutput = true;
                if (gotInput) {
                    inputBeforeOutput = true;
                }

                // verify that there is at most one child element and it is a documentation element
                boolean gotDocumentation2 = false;
                for (Iterator iter2 = XmlUtil.getAllChildren(e2);
                     iter2.hasNext();
                    ) {

                    Element e3 = Util.nextElement(iter2);
                    if (e3 == null)
                        break;

                    if (XmlUtil
                        .matchesTagNS(e3, WSDLConstants.QNAME_DOCUMENTATION)) {
                        if (gotDocumentation2) {
                            Util.fail(
                                "parsing.onlyOneDocumentationAllowed",
                                e.getLocalName());
                        }
                        gotDocumentation2 = true;
                        output.setDocumentation(getDocumentationFor(e3));
                    } else {
                        // possible extensibility element -- must live outside the WSDL namespace
                        checkNotWsdlElement(e3);
                        if (!handleExtension(context, output, e3)) {
                            checkNotWsdlRequired(e3);
                        }
                    }
                }
                context.pop();
            } else if (XmlUtil.matchesTagNS(e2, WSDLConstants.QNAME_FAULT)) {
                context.push();
                context.registerNamespaces(e2);
                BindingFault fault = new BindingFault();
                String nameAttr =
                    Util.getRequiredAttribute(e2, Constants.ATTR_NAME);
                fault.setName(nameAttr);
                operation.addFault(fault);
                gotFault = true;

                // verify that there is at most one child element and it is a documentation element
                boolean gotDocumentation2 = false;
                for (Iterator iter2 = XmlUtil.getAllChildren(e2);
                     iter2.hasNext();
                    ) {
                    Element e3 = Util.nextElement(iter2);
                    if (e3 == null)
                        break;

                    if (XmlUtil
                        .matchesTagNS(e3, WSDLConstants.QNAME_DOCUMENTATION)) {
                        if (gotDocumentation2) {
                            Util.fail(
                                "parsing.onlyOneDocumentationAllowed",
                                e.getLocalName());
                        }
                        gotDocumentation2 = true;
                        if(fault.getDocumentation() == null)
                            fault.setDocumentation(getDocumentationFor(e3));
                    } else {
                        // possible extensibility element -- must live outside the WSDL namespace
                        checkNotWsdlElement(e3);
                        if (!handleExtension(context, fault, e3)) {
                            checkNotWsdlRequired(e3);
                        }
                    }
                }
                context.pop();
            } else {
                // possible extensibility element -- must live outside the WSDL namespace
                checkNotWsdlElement(e2);
                if (!handleExtension(context, operation, e2)) {
                    checkNotWsdlRequired(e2);
                }
            }
        }

        if (gotInput && !gotOutput && !gotFault) {
            operation.setStyle(OperationStyle.ONE_WAY);
        } else if (gotInput && gotOutput && inputBeforeOutput) {
            operation.setStyle(OperationStyle.REQUEST_RESPONSE);
        } else if (gotInput && gotOutput && !inputBeforeOutput) {
            operation.setStyle(OperationStyle.SOLICIT_RESPONSE);
        } else if (gotOutput && !gotInput && !gotFault) {
            operation.setStyle(OperationStyle.NOTIFICATION);
        } else {
            Util.fail("parsing.invalidOperationStyle", name);
        }

        context.pop();
        context.fireDoneParsingEntity(WSDLConstants.QNAME_OPERATION, operation);
        return operation;
    }

    protected Import parseImport(
        TWSDLParserContextImpl context,
        Definitions definitions,
        Element e) {
        context.push();
        context.registerNamespaces(e);
        Import anImport = new Import();
        String namespace =
            Util.getRequiredAttribute(e, Constants.ATTR_NAMESPACE);
        anImport.setNamespace(namespace);
        String location = Util.getRequiredAttribute(e, Constants.ATTR_LOCATION);
        anImport.setLocation(location);

        // according to the schema in the WSDL 1.1 spec, an import can have a documentation element
        boolean gotDocumentation = false;

        for (Iterator iter = XmlUtil.getAllChildren(e); iter.hasNext();) {
            Element e2 = Util.nextElement(iter);
            if (e2 == null)
                break;

            if (XmlUtil.matchesTagNS(e2, WSDLConstants.QNAME_DOCUMENTATION)) {
                if (gotDocumentation) {
                    Util.fail(
                        "parsing.onlyOneDocumentationAllowed",
                        e.getLocalName());
                }
                gotDocumentation = true;
                anImport.setDocumentation(getDocumentationFor(e2));
            } else {
                Util.fail(
                    "parsing.invalidElement",
                    e2.getTagName(),
                    e2.getNamespaceURI());
            }
        }
        context.pop();
        context.fireDoneParsingEntity(WSDLConstants.QNAME_IMPORT, anImport);
        return anImport;
    }

    protected Service parseService(
        TWSDLParserContextImpl context,
        Definitions definitions,
        Element e) {
        context.push();
        context.registerNamespaces(e);
        Service service = new Service(definitions);
        String name = Util.getRequiredAttribute(e, Constants.ATTR_NAME);
        service.setName(name);

        boolean gotDocumentation = false;

        for (Iterator iter = XmlUtil.getAllChildren(e); iter.hasNext();) {
            Element e2 = Util.nextElement(iter);
            if (e2 == null)
                break;

            if (XmlUtil.matchesTagNS(e2, WSDLConstants.QNAME_DOCUMENTATION)) {
                if (gotDocumentation) {
                    Util.fail(
                        "parsing.onlyOneDocumentationAllowed",
                        e.getLocalName());
                }
                gotDocumentation = true;
                if(service.getDocumentation() == null)
                    service.setDocumentation(getDocumentationFor(e2));
            } else if (XmlUtil.matchesTagNS(e2, WSDLConstants.QNAME_PORT)) {
                Port port = parsePort(context, definitions, e2);
                service.add(port);
            } else {
                // possible extensibility element -- must live outside the WSDL namespace
                checkNotWsdlElement(e2);
                if (!handleExtension(context, service, e2)) {
                    checkNotWsdlRequired(e2);
                }
            }
        }

        context.pop();
        context.fireDoneParsingEntity(WSDLConstants.QNAME_SERVICE, service);
        return service;
    }

    protected Port parsePort(
        TWSDLParserContextImpl context,
        Definitions definitions,
        Element e) {
        context.push();
        context.registerNamespaces(e);

        Port port = new Port(definitions);
        String name = Util.getRequiredAttribute(e, Constants.ATTR_NAME);
        port.setName(name);

        String bindingAttr =
            Util.getRequiredAttribute(e, Constants.ATTR_BINDING);
        port.setBinding(context.translateQualifiedName(bindingAttr));

        boolean gotDocumentation = false;

        for (Iterator iter = XmlUtil.getAllChildren(e); iter.hasNext();) {
            Element e2 = Util.nextElement(iter);
            if (e2 == null)
                break;

            if (XmlUtil.matchesTagNS(e2, WSDLConstants.QNAME_DOCUMENTATION)) {
                if (gotDocumentation) {
                    Util.fail(
                        "parsing.onlyOneDocumentationAllowed",
                        e.getLocalName());
                }
                gotDocumentation = true;
                if(port.getDocumentation() == null)
                    port.setDocumentation(getDocumentationFor(e2));
            } else {
                // possible extensibility element -- must live outside the WSDL namespace
                checkNotWsdlElement(e2);
                if (!handleExtension(context, port, e2)) {
                    checkNotWsdlRequired(e2);
                }
            }
        }

        context.pop();
        context.fireDoneParsingEntity(WSDLConstants.QNAME_PORT, port);
        return port;
    }

    private void addSchemaElements(Element typesElement){
        boolean gotDocumentation = false;

        for (Iterator iter = XmlUtil.getAllChildren(typesElement); iter.hasNext();) {
            Element e = Util.nextElement(iter);
            if (e == null)
                break;
            if (gotDocumentation) {
                Util.fail(
                    "parsing.onlyOneDocumentationAllowed",
                    e.getLocalName());
            }
            if (XmlUtil.matchesTagNS(e, SchemaConstants.QNAME_IMPORT)) {
                env.warn(WsdlMessages.localizableWARNING_WSI_R_2003());
            }else{
                checkNotWsdlElement(e);
                if (XmlUtil.matchesTagNS(e, SchemaConstants.QNAME_SCHEMA)) {
                    schemaElements.add(e);
                }
            }
        }
    }

    public List getSchemaElements(){
        return schemaElements;
    }

    protected boolean handleExtension(
        TWSDLParserContextImpl context,
        TWSDLExtensible entity,
        Element e) {
        TWSDLExtensionHandler h =
             (TWSDLExtensionHandler) extensionHandlers.get(e.getNamespaceURI());
        if (h == null) {
            context.fireIgnoringExtension(
                new QName(e.getNamespaceURI(), e.getLocalName()),
                ((Entity) entity).getElementName());
            return false;
        } else {
            return h.doHandleExtension(context, entity, e);
        }
    }

    protected boolean handleExtension(
        TWSDLParserContextImpl context,
        TWSDLExtensible entity,
        Node n,
        Element e) {
        TWSDLExtensionHandler h =
            (TWSDLExtensionHandler) extensionHandlers.get(n.getNamespaceURI());
        if (h == null) {
            context.fireIgnoringExtension(
                new QName(n.getNamespaceURI(), n.getLocalName()),
                ((Entity) entity).getElementName());
            return false;
        } else {
            return h.doHandleExtension(context, entity, e);
        }
    }

    protected void checkNotWsdlElement(Element e) {
        // possible extensibility element -- must live outside the WSDL namespace
        if (e.getNamespaceURI().equals(Constants.NS_WSDL))
            Util.fail("parsing.invalidWsdlElement", e.getTagName());
    }

    protected void checkNotWsdlAttribute(Attr a) {
        // possible extensibility element -- must live outside the WSDL namespace
        if (a.getNamespaceURI().equals(Constants.NS_WSDL))
            Util.fail("parsing.invalidWsdlElement", a.getLocalName());
    }

    protected void checkNotWsdlRequired(Element e) {
        // check the wsdl:required attribute, fail if set to "true"
        String required =
            XmlUtil.getAttributeNSOrNull(
                e,
                Constants.ATTR_REQUIRED,
                Constants.NS_WSDL);
        if (required != null && required.equals(Constants.TRUE)) {
            Util.fail(
                "parsing.requiredExtensibilityElement",
                e.getTagName(),
                e.getNamespaceURI());
        }
    }

    protected Documentation getDocumentationFor(Element e) {
        String s = XmlUtil.getTextForNode(e);
        if (s == null) {
            return null;
        } else {
            return new Documentation(s);
        }
    }
}
