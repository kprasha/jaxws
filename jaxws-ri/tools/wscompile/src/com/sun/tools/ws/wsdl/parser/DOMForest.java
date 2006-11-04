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

import com.sun.tools.ws.resources.WsdlMessages;
import com.sun.tools.ws.wscompile.ErrorReceiver;
import com.sun.tools.ws.wscompile.WsimportOptions;
import com.sun.tools.ws.wsdl.document.WSDLConstants;
import com.sun.tools.ws.wsdl.document.jaxws.JAXWSBindingsConstants;
import com.sun.tools.ws.wsdl.document.schema.SchemaConstants;
import com.sun.tools.ws.wsdl.framework.ParseException;
import com.sun.tools.xjc.reader.internalizer.LocatorTable;
import com.sun.xml.bind.marshaller.DataWriter;
import com.sun.xml.ws.api.wsdl.parser.MetaDataResolver;
import com.sun.xml.ws.api.wsdl.parser.MetadataResolverFactory;
import com.sun.xml.ws.api.wsdl.parser.ServiceDescriptor;
import com.sun.xml.ws.util.DOMUtil;
import com.sun.xml.ws.util.ServiceFinder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.LocatorImpl;
import org.xml.sax.helpers.XMLFilterImpl;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * @author Vivek Pandey
 */
public class DOMForest {
    /**
     * To correctly feed documents to a schema parser, we need to remember
     * which documents (of the forest) were given as the root
     * documents, and which of them are read as included/imported
     * documents.
     * <p/>
     * <p/>
     * Set of system ids as strings.
     */
    private final Set<String> rootDocuments = new HashSet<String>();

    /**
     * Contains wsdl:import(s)
     */
    private final Set<String> externalReferences = new HashSet<String>();

    /**
     * actual data storage map&lt;SystemId,Document>.
     */
    private final Map<String, Document> core = new HashMap<String, Document>();
    private final WsimportOptions options;
    private final DocumentBuilder documentBuilder;
    private final SAXParserFactory parserFactory;
    private ErrorReceiver errorReceiver;

    /** inlined schema elements inside wsdl:type section */
    private final List<Element> inlinedSchemaElements = new ArrayList<Element>();


    /**
     * Stores location information for all the trees in this forest.
     */
    public final LocatorTable locatorTable = new LocatorTable();

    /**
     * Stores all the outer-most &lt;jaxb:bindings> customizations.
     */
    public final Set<Element> outerMostBindings = new HashSet<Element>();

    /**
     * Schema language dependent part of the processing.
     */
    protected final InternalizationLogic logic;

    public DOMForest(InternalizationLogic logic, WsimportOptions options, ErrorReceiver errReceiver) {
        this.options = options;
        this.errorReceiver = errReceiver;
        this.logic = logic;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            this.documentBuilder = dbf.newDocumentBuilder();

            this.parserFactory = SAXParserFactory.newInstance();
            this.parserFactory.setNamespaceAware(true);
        } catch (ParserConfigurationException e) {
            throw new AssertionError(e);
        }
    }

    public List<Element> getInlinedSchemaElement(){
        return inlinedSchemaElements;
    }

    public Document parse(InputSource source, boolean root) throws SAXException {
        if (source.getSystemId() == null)
            throw new IllegalArgumentException();

        return parse(source.getSystemId(), source, root);
    }

    /**
     * Parses an XML at the given location (
     * and XMLs referenced by it) into DOM trees
     * and stores them to this forest.
     *
     * @return the parsed DOM document object.
     */
    public Document parse(String systemId, boolean root) throws SAXException, IOException {

        systemId = normalizeSystemId(systemId);

        if (core.containsKey(systemId)){
            // this document has already been parsed. Just ignore.
            return core.get(systemId);
        }


        InputSource is = null;

        // allow entity resolver to find the actual byte stream.
        if (options.entityResolver != null)
            is = options.entityResolver.resolveEntity(null, systemId);
        if (is == null)
            is = new InputSource(systemId);

        // but we still use the original system Id as the key.
        return parse(systemId, is, root);
    }

    /**
     * Parses the given document and add it to the DOM forest.
     *
     * @return null if there was a parse error. otherwise non-null.
     */
    public Document parse(String systemId, InputSource inputSource, boolean root) throws SAXException {
        Document dom = documentBuilder.newDocument();

        systemId = normalizeSystemId(systemId);

        boolean retryMex = false;
        Exception exception = null;
        // put into the map before growing a tree, to
        // prevent recursive reference from causing infinite loop.
        core.put(systemId, dom);

        dom.setDocumentURI(systemId);
        if (root)
            rootDocuments.add(systemId);

        try {
            XMLReader reader = parserFactory.newSAXParser().getXMLReader();
            reader.setContentHandler(getParserHandler(dom));
            if (errorReceiver != null)
                reader.setErrorHandler(errorReceiver);
            if (options.entityResolver != null)
                reader.setEntityResolver(options.entityResolver);
            reader.parse(inputSource);
            Element doc = dom.getDocumentElement();
            if(doc == null){
                return null;
            }
            //if its JAXWS binding file just return it
            if(doc.getNamespaceURI() != null && doc.getNamespaceURI().equals(JAXWSBindingsConstants.JAXWS_BINDINGS.getNamespaceURI()) &&
                    doc.getLocalName().equals(JAXWSBindingsConstants.JAXWS_BINDINGS.getLocalPart()))
                    return dom;

            if(doc.getNamespaceURI() != null && doc.getNamespaceURI().equals(SchemaConstants.NS_XSD))
                return dom;


            //if its not a WSDL document, retry with MEX
            if (doc.getNamespaceURI() == null || !doc.getNamespaceURI().equals(WSDLConstants.NS_WSDL) || !doc.getLocalName().equals("definitions")) {
                retryMex = true;
            }
            NodeList schemas = doc.getElementsByTagNameNS(SchemaConstants.NS_XSD, "schema");
            for (int i = 0; i < schemas.getLength(); i++) {
                inlinedSchemaElements.add((Element) schemas.item(i));
            }
        } catch (ParserConfigurationException e) {
            retryMex = true;
            exception = e;
        } catch (IOException e) {            
            //lets try with MetadataResolverFactory
            retryMex = true;
            exception = e;
        }catch(SAXException e){
            //lets try with MetadataResolverFactory
            retryMex = true;
            exception = e;

        }
        
        if(retryMex){
            if(dom.getDocumentElement() != null){
                Element doc = dom.getDocumentElement();
                errorReceiver.warning(locatorTable.getStartLocation(doc), WsdlMessages.INVALID_WSDL_WITH_DOOC(systemId, "{"+doc.getNamespaceURI()+"}"+doc.getLocalName()));
            }else{
                errorReceiver.warning(new LocatorImpl(), WsdlMessages.INVALID_WSDL(systemId));
            }
            dom =  getFromMetadataResolver(systemId);
            //mex succedded
            if(dom != null){
                return dom;
            }
        }
        if(exception != null){
            errorReceiver.error(exception.getMessage(), exception);
            core.remove(systemId);
            rootDocuments.remove(systemId);
        }
        return dom;
    }

    public void addExternalReferences(String ref) {
        if(!externalReferences.contains(ref))
            externalReferences.add(ref);
    }


    public Set<String> getExternalReferences() {
        return externalReferences;
    }

    public interface Handler extends ContentHandler {
        /**
         * Gets the DOM that was built.
         */
        public Document getDocument();
    }

    private static abstract class HandlerImpl extends XMLFilterImpl implements Handler {
    }

    /**
     * Returns a {@link ContentHandler} to feed SAX events into.
     * <p>
     * The client of this class can feed SAX events into the handler
     * to parse a document into this DOM forest.
     */
    public Handler getParserHandler(String systemId, boolean root) {
        final Document dom = documentBuilder.newDocument();
        core.put(systemId, dom);
        if (root)
            rootDocuments.add(systemId);

        ContentHandler handler = getParserHandler(dom);

        // we will register the DOM to the map once the system ID becomes available.
        // but the SAX allows the event source to not to provide that information,
        // so be prepared for such case.
        HandlerImpl x = new HandlerImpl() {
            public Document getDocument() {
                return dom;
            }
        };
        x.setContentHandler(handler);

        return x;
    }

    /**
     * Returns a {@link org.xml.sax.ContentHandler} to feed SAX events into.
     * <p/>
     * <p/>
     * The client of this class can feed SAX events into the handler
     * to parse a document into this DOM forest.
     * <p/>
     * This version requires that the DOM object to be created and registered
     * to the map beforehand.
     */
    private ContentHandler getParserHandler(Document dom) {
        ContentHandler handler = new DOMBuilder(dom, locatorTable, outerMostBindings);
        handler = new WhitespaceStripper(handler, errorReceiver, options.entityResolver);
        handler = new VersionChecker(handler, errorReceiver, options.entityResolver);

        // insert the reference finder so that
        // included/imported schemas will be also parsed
        XMLFilterImpl f = logic.createExternalReferenceFinder(this);
        f.setContentHandler(handler);

        if (errorReceiver != null)
            f.setErrorHandler(errorReceiver);
        if (options.entityResolver != null)
            f.setEntityResolver(options.entityResolver);

        return f;
    }

    private String normalizeSystemId(String systemId) {
        try {
            systemId = new URI(systemId).normalize().toString();
        } catch (URISyntaxException e) {
            // leave the system ID untouched. In my experience URI is often too strict
        }
        return systemId;
    }

    boolean isExtensionMode() {
        return options.isExtensionMode();
    }

    /*
     * If source and target namespace are also passed in,
     * then if the mex resolver is found and it cannot get
     * the data, wsimport attempts to add ?wsdl to the
     * address and retrieve the data with a normal http get.
     * This behavior should only happen when trying a
     * mex request first.
     */
    private Document getFromMetadataResolver(String systemId){

        //try MEX
        MetaDataResolver resolver = null;
        ServiceDescriptor serviceDescriptor = null;
        for(MetadataResolverFactory resolverFactory: ServiceFinder.find(MetadataResolverFactory.class)){
            resolver = resolverFactory.metadataResolver(options.entityResolver);
            try {
                serviceDescriptor = resolver.resolve(new URI(systemId));
                //we got the ServiceDescriptor, now break
                if(serviceDescriptor != null)
                    break;
            } catch (URISyntaxException e) {
                throw new ParseException(e);
            }
        }

        if(serviceDescriptor != null){
            return parseMetadata(systemId, serviceDescriptor);
        } else{
            errorReceiver.error(new LocatorImpl(), WsdlMessages.PARSING_UNABLE_TO_GET_METADATA(systemId));
        }
        return null;
    }

    private Document parseMetadata(String systemId, ServiceDescriptor serviceDescriptor){
        List<? extends Source> wsdls = serviceDescriptor.getWSDLs();
        List<? extends Source> schemas = serviceDescriptor.getSchemas();
        Document root = null;
        for(Source src:wsdls){
            if(src instanceof DOMSource){
                Node n = ((DOMSource) src).getNode();
                Document doc;
                if(n.getNodeType() == Node.ELEMENT_NODE && n.getOwnerDocument() == null){
                    doc = DOMUtil.createDom();
                    doc.importNode(n, true);
                }else{
                    doc = n.getOwnerDocument();
                }

//                Element e = (n.getNodeType() == Node.ELEMENT_NODE)?(Element)n: DOMUtil.getFirstElementChild(n);
                if(root == null){
                    //check if its main wsdl, then set it to root
                    NodeList nl = doc.getDocumentElement().getElementsByTagNameNS(WSDLConstants.NS_WSDL, "service");
                    if(nl.getLength() > 0){
                        root = doc;
                        mexRootDoc = src.getSystemId();
                    }
                }
                NodeList nl = doc.getDocumentElement().getElementsByTagNameNS(WSDLConstants.NS_WSDL, "import");
                if(nl.getLength() > 0){
                    Element imp = (Element) nl.item(0);
                    String loc = imp.getAttribute("location");
                    if(loc != null){
                        if(!externalReferences.contains(loc))
                            externalReferences.add(loc);
                    }
                }
                if(core.keySet().contains(systemId))
                    core.remove(systemId);
                core.put(src.getSystemId(), doc);
                isMexMetadata = true;
            }

            //TODO:handle SAXSource
            //TODO:handler StreamSource
        }

        for(Source src:schemas){
            if(src instanceof DOMSource){
                Node n = ((DOMSource) src).getNode();
                Element e = (n.getNodeType() == Node.ELEMENT_NODE)?(Element)n:DOMUtil.getFirstElementChild(n);
                inlinedSchemaElements.add(e);
            }
            //TODO:handle SAXSource
            //TODO:handler StreamSource
        }
        return root;
    }


    public boolean isMexMetadata;
    private String mexRootDoc;
    public Document getMexRootWSDL(){
        return get(mexRootDoc);
    }

    /**
     * Gets the DOM tree associated with the specified system ID,
     * or null if none is found.
     */
    public Document get( String systemId ) {
        Document doc = core.get(systemId);

        if( doc==null && systemId.startsWith("file:/") && !systemId.startsWith("file://") ) {
            // As of JDK1.4, java.net.URL.toExternal method returns URLs like
            // "file:/abc/def/ghi" which is an incorrect file protocol URL according to RFC1738.
            // Some other correctly functioning parts return the correct URLs ("file:///abc/def/ghi"),
            // and this descripancy breaks DOM look up by system ID.

            // this extra check solves this problem.
            doc = core.get( "file://"+systemId.substring(5) );
        }

        if( doc==null && systemId.startsWith("file:") ) {
            // on Windows, filenames are case insensitive.
            // perform case-insensitive search for improved user experience
            String systemPath = getPath(systemId);
            for (String key : core.keySet()) {
                if(key.startsWith("file:") && getPath(key).equalsIgnoreCase(systemPath)) {
                    doc = core.get(key);
                    break;
                }
            }
        }

        return doc;
    }

    /**
     * Strips off the leading 'file:///' portion from an URL.
     */
    private String getPath(String key) {
        key = key.substring(5); // skip 'file:'
        while(key.length()>0 && key.charAt(0)=='/')
            key = key.substring(1);
        return key;
    }

    /**
     * Gets all the system IDs of the documents.
     */
    public String[] listSystemIDs() {
        return core.keySet().toArray(new String[core.keySet().size()]);
    }

    /**
     * Gets the system ID from which the given DOM is parsed.
     * <p>
     * Poor-man's base URI.
     */
    public String getSystemId( Document dom ) {
        for (Map.Entry<String,Document> e : core.entrySet()) {
            if (e.getValue() == dom)
                return e.getKey();
        }
        return null;
    }

    /**
     * Dumps the contents of the forest to the specified stream.
     *
     * This is a debug method. As such, error handling is sloppy.
     */
    public void dump( OutputStream out ) throws IOException {
        try {
            // create identity transformer
            Transformer it = TransformerFactory.newInstance().newTransformer();

            for (Map.Entry<String, Document> e : core.entrySet()) {
                out.write( ("---<< "+e.getKey()+'\n').getBytes() );

                DataWriter dw = new DataWriter(new OutputStreamWriter(out),null);
                dw.setIndentStep("  ");
                it.transform( new DOMSource(e.getValue()),
                    new SAXResult(dw));

                out.write( "\n\n\n".getBytes() );
            }
        } catch( TransformerException e ) {
            e.printStackTrace();
        }
    }

}
