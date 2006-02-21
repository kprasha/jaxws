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

import com.sun.tools.ws.api.wsdl.TWSDLExtensionHandler;
import com.sun.xml.ws.util.ServiceFinder;
import static com.sun.xml.ws.util.xml.XmlUtil.DRACONIAN_ERROR_HANDLER;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.dom.DOMSource;
import org.w3c.dom.Attr;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.EntityResolver;

import com.sun.xml.ws.util.localization.LocalizableMessageFactory;
import com.sun.xml.ws.util.localization.Localizer;
import com.sun.xml.ws.util.JAXWSUtils;
import com.sun.tools.ws.util.xml.NullEntityResolver;
import com.sun.tools.ws.wsdl.document.Binding;
import com.sun.tools.ws.wsdl.document.BindingFault;
import com.sun.tools.ws.wsdl.document.BindingInput;
import com.sun.tools.ws.wsdl.document.BindingOperation;
import com.sun.tools.ws.wsdl.document.BindingOutput;
import com.sun.tools.ws.wsdl.document.Definitions;
import com.sun.tools.ws.wsdl.document.Documentation;
import com.sun.tools.ws.wsdl.document.Fault;
import com.sun.tools.ws.wsdl.document.Import;
import com.sun.tools.ws.wsdl.document.Input;
import com.sun.tools.ws.wsdl.document.Message;
import com.sun.tools.ws.wsdl.document.MessagePart;
import com.sun.tools.ws.wsdl.document.Operation;
import com.sun.tools.ws.wsdl.document.OperationStyle;
import com.sun.tools.ws.wsdl.document.Output;
import com.sun.tools.ws.wsdl.document.Port;
import com.sun.tools.ws.wsdl.document.PortType;
import com.sun.tools.ws.wsdl.document.Service;
import com.sun.tools.ws.wsdl.document.Types;
import com.sun.tools.ws.wsdl.document.WSDLConstants;
import com.sun.tools.ws.wsdl.document.WSDLDocument;
import com.sun.tools.ws.wsdl.document.schema.SchemaConstants;
import com.sun.tools.ws.wsdl.document.schema.SchemaKinds;
import com.sun.tools.ws.wsdl.framework.Entity;
import com.sun.tools.ws.wsdl.mex.HTTPMexClient;
import com.sun.tools.ws.wsdl.wxf.HTTPWxfClient;
import com.sun.tools.ws.api.wsdl.TWSDLExtensible;
import com.sun.tools.ws.api.wsdl.TWSDLExtensionHandler;
import com.sun.tools.ws.wsdl.framework.ParseException;
import com.sun.tools.ws.wsdl.framework.TWSDLParserContextImpl;
import com.sun.tools.ws.wsdl.framework.ParserListener;
import com.sun.tools.ws.util.xml.XmlUtil;
import com.sun.tools.ws.processor.util.ProcessorEnvironment;
import com.sun.tools.ws.processor.config.WSDLModelInfo;

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
    private final Map<String, Document> wsdlDocuments = new HashMap<String, Document>();

    private WSDLParser() {
        _extensionHandlers = new HashMap();
        hSet = new HashSet();

        // register handlers for default extensions
        register(new SOAPExtensionHandler(_extensionHandlers));
        register(new HTTPExtensionHandler(_extensionHandlers));
        register(new MIMEExtensionHandler(_extensionHandlers));
        //we dont really need to handle the schema it done latteron by JAXB
        //TODO: verify it and if not needed remove SchemaExtensionHandler
        register(new SchemaExtensionHandler(_extensionHandlers));
        register(new JAXWSBindingExtensionHandler(_extensionHandlers));
        register(new SOAP12ExtensionHandler(_extensionHandlers));
        
        for (TWSDLExtensionHandler te : ServiceFinder.find(TWSDLExtensionHandler.class).toArray()) {
            register(te);
        }
    }

    public WSDLParser(WSDLModelInfo modelInfo) {
        this();
        assert(modelInfo != null);
        this.modelInfo = modelInfo;
        this.entityResolver = modelInfo.getEntityResolver();
    }

    public void register(TWSDLExtensionHandler h) {
        _extensionHandlers.put(h.getNamespaceURI(), h);
    }

    public void unregister(TWSDLExtensionHandler h) {
        _extensionHandlers.put(h.getNamespaceURI(), null);
    }

    public void unregister(String uri) {
        _extensionHandlers.put(uri, null);
    }

    public boolean getFollowImports() {
        return _followImports;
    }

    public void setFollowImports(boolean b) {
        _followImports = b;
    }

    public void addParserListener(ParserListener l) {
        if (_listeners == null) {
            _listeners = new ArrayList();
        }
        _listeners.add(l);
    }

    public void removeParserListener(ParserListener l) {
        if (_listeners == null) {
            return;
        }
        _listeners.remove(l);
    }

    public boolean getUseMex() {
        return useMex;
    }

    public void setUseMex(boolean b) {
        useMex = b;
    }

    public boolean getUseWxf() {
        return useWxf;
    }

    public void setUseWxf(boolean b) {
        useWxf = b;
    }

//    public WSDLDocument parse(InputSource source) {
//        _messageFactory =
//            new LocalizableMessageFactory("com.sun.tools.ws.resources.wsdl");
//        _localizer = new Localizer();
//
//        WSDLDocument document = new WSDLDocument();
//        document.setSystemId(source.getSystemId());
//        TWSDLParserContextImpl context = new TWSDLParserContextImpl(document, _listeners);
//        context.setFollowImports(_followImports);
//        document.setDefinitions(parseDefinitions(context, source, null));
//        return document;
//    }

    public WSDLDocument parse(){
        String location = modelInfo.getLocation();
        assert(location != null);
        _messageFactory =
            new LocalizableMessageFactory("com.sun.tools.ws.resources.wsdl");
        _localizer = new Localizer();

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
        TWSDLParserContextImpl context = new TWSDLParserContextImpl(document, _listeners);
        context.setFollowImports(_followImports);
        document.setDefinitions(parseDefinitions(context, source, null));
        return document;
    }

    protected Definitions parseDefinitions(TWSDLParserContextImpl context,
            InputSource source, String expectedTargetNamespaceURI) {
        context.pushWSDLLocation();
        context.setWSDLLocation(context.getDocument().getSystemId());
        String sysId = context.getDocument().getSystemId();
        buildDocumentFromWSDL(sysId, source, expectedTargetNamespaceURI);
        Document root = wsdlDocuments.get(sysId);

        //Internalizer.transform takes Set of jaxws:bindings elements, this is to allow multiple external
        //bindings to be transformed.
        new Internalizer().transform(modelInfo.getJAXWSBindings(), wsdlDocuments,
                (ProcessorEnvironment)modelInfo.getParent().getEnvironment());

        //print the wsdl
//        try{
//            dump(System.out);
//        }catch(IOException e){
//            e.printStackTrace();
//        }

        Definitions definitions = parseDefinitionsNoImport(context, root, expectedTargetNamespaceURI);
        processImports(context, source, definitions);
        context.popWSDLLocation();
        return definitions;
    }

    /**
     * @param systemId
     * @param source
     * @param expectedTargetNamespaceURI
     */
    private void buildDocumentFromWSDL(String systemId, InputSource source, String expectedTargetNamespaceURI) {
        try {
            Document document = null;
            if (useMex && systemId.startsWith("http")) {
                document = getMexClient().getWSDLDocument(systemId);
            } else if (useWxf && systemId.startsWith("http")) {
                document = getWxfClient().getWSDLDocument(systemId);
            } else {
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
            }
            wsdlDocuments.put(systemId, document);
            Element e = document.getDocumentElement();
            Util.verifyTagNSRootElement(e, WSDLConstants.QNAME_DEFINITIONS);
            String name = XmlUtil.getAttributeOrNull(e, Constants.ATTR_NAME);

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
        } catch (IOException e) {
            if (source.getSystemId() != null) {
                throw new ParseException(
                    "parsing.ioExceptionWithSystemId",
                    source.getSystemId(),e);
            } else {
                throw new ParseException("parsing.ioException",e);
            }
        } catch (SAXException e) {
            if (source.getSystemId() != null) {
                throw new ParseException(
                    "parsing.saxExceptionWithSystemId",
                    source.getSystemId(),e);
            } else {
                throw new ParseException("parsing.saxException",e);
            }
        } catch (ParserConfigurationException e) {
            throw new ParseException(
                "parsing.parserConfigException",e);
        } catch (FactoryConfigurationError e) {
            throw new ParseException(
                "parsing.factoryConfigException",e);
        }
    }

    /**
     * @param source
     * @param location
     */
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

    /* (non-Javadoc)
     * @see WSDLParser#processImports(TWSDLParserContextImpl, org.xml.sax.InputSource, Definitions)
     */
    protected void processImports(TWSDLParserContextImpl context, InputSource source, Definitions definitions) {
        for(String location : imports){
            if (!context.getDocument().isImportedDocument(location)){
                Definitions importedDefinitions = parseDefinitionsNoImport(context,
                        wsdlDocuments.get(location), location);
                if(importedDefinitions == null)
                    continue;
                context.getDocument().addImportedEntity(importedDefinitions);
                context.getDocument().addImportedDocument(location);
            }
        }
    }

    protected Definitions parseDefinitionsNoImport(
        TWSDLParserContextImpl context,
        InputSource source,
        String expectedTargetNamespaceURI) {
        try {
            DocumentBuilderFactory builderFactory =
                DocumentBuilderFactory.newInstance();
            builderFactory.setNamespaceAware(true);
            builderFactory.setValidating(false);
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            builder.setErrorHandler(DRACONIAN_ERROR_HANDLER);
            builder.setEntityResolver(new NullEntityResolver());

            try {
                Document document = builder.parse(source);
                return parseDefinitionsNoImport(
                    context,
                    document,
                    expectedTargetNamespaceURI);
            } catch (IOException e) {
                if (source.getSystemId() != null) {
                    throw new ParseException(
                        "parsing.ioExceptionWithSystemId",
                        source.getSystemId(),e);
                } else {
                    throw new ParseException("parsing.ioException",e);
                }
            } catch (SAXException e) {
                if (source.getSystemId() != null) {
                    throw new ParseException(
                        "parsing.saxExceptionWithSystemId",
                        source.getSystemId(),
                        e);
                } else {
                    throw new ParseException("parsing.saxException",e);
                }
            }
        } catch (ParserConfigurationException e) {
            throw new ParseException("parsing.parserConfigException",e);
        } catch (FactoryConfigurationError e) {
            throw new ParseException("parsing.factoryConfigException",e);
        }
    }

    protected Definitions parseDefinitionsNoImport(
        TWSDLParserContextImpl context,
        Document doc,
        String expectedTargetNamespaceURI) {
        _targetNamespaceURI = null;
        Element root = doc.getDocumentElement();
        Util.verifyTagNSRootElement(root, WSDLConstants.QNAME_DEFINITIONS);
        return parseDefinitionsNoImport(
            context,
            root,
            expectedTargetNamespaceURI);
    }

    protected Definitions parseDefinitionsNoImport(
        TWSDLParserContextImpl context,
        Element e,
        String expectedTargetNamespaceURI) {
        context.push();
        context.registerNamespaces(e);

        Definitions definitions = new Definitions(context.getDocument());
        String name = XmlUtil.getAttributeOrNull(e, Constants.ATTR_NAME);
        definitions.setName(name);

        _targetNamespaceURI =
            XmlUtil.getAttributeOrNull(e, Constants.ATTR_TARGET_NAMESPACE);

        definitions.setTargetNamespaceURI(_targetNamespaceURI);

        boolean gotDocumentation = false;
        boolean gotTypes = false;

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
                if(definitions.getDocumentation() == null)
                    definitions.setDocumentation(getDocumentationFor(e2));
            } else if (XmlUtil.matchesTagNS(e2, WSDLConstants.QNAME_TYPES)) {
                if (gotTypes) {
                    Util.fail(
                        "parsing.onlyOneTypesAllowed",
                        Constants.TAG_DEFINITIONS);
                }
                //add all the wsdl:type elements to latter make a list of all the schema elements
                // that will be needed to create jaxb model
                addSchemaElements(e2);

                //definitions.setTypes(parseTypes(context, definitions, e2));
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
            } else if (
                (_useWSIBasicProfile)
                    && (XmlUtil.matchesTagNS(e2, SchemaConstants.QNAME_IMPORT))) {
                warn("warning.wsi.r2003");
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
                if (hSet.isEmpty()) {
                    hSet.add(ee.getAttribute("use"));
                } else {
                    /* this codition will happen when the wsdl used has a mixture of
                       literal and encoded style */
                    if (!hSet.contains(ee.getAttribute("use"))
                        && (ee.getAttribute("use") != "")) {
                        hSet.add(ee.getAttribute("use"));
                    }
                }

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

    protected Types parseTypes(
        TWSDLParserContextImpl context,
        Definitions definitions,
        Element e) {
        context.push();
        context.registerNamespaces(e);
        Types types = new Types();

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
                types.setDocumentation(getDocumentationFor(e2));
            } //bug fix 4854004
            else if (
                (_useWSIBasicProfile)
                    && (XmlUtil.matchesTagNS(e2, SchemaConstants.QNAME_IMPORT))) {
                warn("warning.wsi.r2003");
            } else {
                // possible extensibility element -- must live outside the WSDL namespace
                checkNotWsdlElement(e2);
                try {
                    if (!handleExtension(context, types, e2)) {
                        checkNotWsdlRequired(e2);
                    }
                } catch (ParseException pe) {
                    if (pe.getKey().equals("parsing.incorrectRootElement")) {
                        if (_useWSIBasicProfile) {
                            warn("warning.wsi.r2004");
                        }
                        throw pe;
                    }
                }
            }
        }

        context.pop();
        context.fireDoneParsingEntity(WSDLConstants.QNAME_TYPES, types);
        return types;
    }

    private List _elements = new ArrayList();

    public void addSchemaElements(Element typesElement){
        for (Iterator iter = XmlUtil.getAllChildren(typesElement); iter.hasNext();) {
            Element e = Util.nextElement(iter);
            if (e == null)
                break;

            if (XmlUtil.matchesTagNS(e, SchemaConstants.QNAME_SCHEMA)) {
                _elements.add(e);
            } else {
                // possible extensibility element -- must live outside the WSDL namespace
                checkNotWsdlElement(e);
            }
        }
    }

    public List getSchemaElements(){
        return _elements;
    }

    protected boolean handleExtension(
        TWSDLParserContextImpl context,
        TWSDLExtensible entity,
        Element e) {
        TWSDLExtensionHandler h =
             (TWSDLExtensionHandler) _extensionHandlers.get(e.getNamespaceURI());
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
            (TWSDLExtensionHandler) _extensionHandlers.get(n.getNamespaceURI());
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

    protected void error(String key) {
        System.err.println(
            _localizer.localize(_messageFactory.getMessage(key)));
    }

    public HashSet getUse() {
        return hSet;
    }

    protected void warn(String key) {
        System.err.println(
            _localizer.localize(_messageFactory.getMessage(key)));
    }

    protected void warn(String key, String arg) {
        System.err.println(
            _localizer.localize(_messageFactory.getMessage(key, arg)));
    }

    protected void warn(String key, Object[] args) {
        System.err.println(
            _localizer.localize(_messageFactory.getMessage(key, args)));
    }
    
    /*
     * Used to avoid creating a new one every time
     * a request is made.
     */
    private HTTPMexClient getMexClient() {
        if (mexClient == null) {
            mexClient = new HTTPMexClient();
        }
        return mexClient;
    }

    /*
     * Used to avoid creating a new one every time
     * a request is made.
     */
    private HTTPWxfClient getWxfClient() {
        if (wxfClient == null) {
            wxfClient = new HTTPWxfClient();
        }
        return wxfClient;
    }

    private boolean _followImports;
    private String _targetNamespaceURI;
    private Map _extensionHandlers;
    private ArrayList _listeners;
    private boolean _useWSIBasicProfile = true;
    private LocalizableMessageFactory _messageFactory = null;
    private Localizer _localizer;
    private HashSet hSet = null;
    private boolean useMex = false;
    private HTTPMexClient mexClient;
    private boolean useWxf = false;
    private HTTPWxfClient wxfClient;
}
