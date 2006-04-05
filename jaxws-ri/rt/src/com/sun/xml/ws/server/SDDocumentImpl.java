package com.sun.xml.ws.server;

import com.sun.xml.ws.api.server.DocumentAddressResolver;
import com.sun.xml.ws.api.server.PortAddressResolver;
import com.sun.xml.ws.api.server.SDDocument;
import com.sun.xml.ws.api.server.SDDocumentSource;
import com.sun.xml.ws.api.server.WSEndpoint;
import com.sun.xml.ws.streaming.XMLStreamReaderUtil;
import com.sun.xml.ws.wsdl.parser.WSDLConstants;
import com.sun.xml.ws.wsdl.parser.ParserUtil;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamConstants;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;

/**
 * {@link SDDocument} implmentation.
 *
 * <p>
 * This extends from {@link SDDocumentSource} so that
 * JAX-WS server runtime code can use {@link SDDocument}
 * as {@link SDDocumentSource}.
 *
 * @author Kohsuke Kawaguchi
 */
class SDDocumentImpl extends SDDocumentSource implements SDDocument {

    /**
     * Creates {@link SDDocument} from {@link SDDocumentSource}.
     *
     * @param serviceName
     * @param portTypeName
     *      The information about the port of {@link WSEndpoint} to which this document is built for.
     *      These values are used to determine which document is the concrete and abstract WSDLs
     *      for this endpoint.
     *
     * @return null
     *      Always non-null.
     */
    public static SDDocumentImpl create(SDDocumentSource src, QName serviceName, QName portTypeName) {
        URL systemId = src.getSystemId();

        try {
            // RuntimeWSDLParser parser = new RuntimeWSDLParser(null);
            XMLStreamReader reader = src.read(xif);
            try {
                XMLStreamReaderUtil.nextElementContent(reader);

                QName rootName = reader.getName();
                if(rootName.equals(WSDLConstants.QNAME_SCHEMA)) {
                    String tns = ParserUtil.getMandatoryNonEmptyAttribute(reader, WSDLConstants.ATTR_TNS);

                    return new SchemaImpl(rootName,systemId,src,tns);
                } else if (rootName.equals(WSDLConstants.QNAME_DEFINITIONS)) {
                    String tns = ParserUtil.getMandatoryNonEmptyAttribute(reader, WSDLConstants.ATTR_TNS);

                    boolean hasPortType = false;
                    boolean hasService = false;

                    // if WSDL, parse more
                    while (XMLStreamReaderUtil.nextElementContent(reader) !=
                            XMLStreamConstants.END_ELEMENT) {
                         if(reader.getEventType() == XMLStreamConstants.END_DOCUMENT)
                            break;

                        QName name = reader.getName();
                        if (WSDLConstants.QNAME_PORT_TYPE.equals(name)) {
                            String pn = ParserUtil.getMandatoryNonEmptyAttribute(reader, WSDLConstants.ATTR_NAME);
                            if (portTypeName != null) {
                                if(portTypeName.getLocalPart().equals(pn)&&portTypeName.getNamespaceURI().equals(tns)) {
                                    hasPortType = true;
                                }
                            }
                            XMLStreamReaderUtil.skipElement(reader);
                        } else if (WSDLConstants.QNAME_SERVICE.equals(name)) {
                            String sn = ParserUtil.getMandatoryNonEmptyAttribute(reader, WSDLConstants.ATTR_NAME);
                            QName sqn = new QName(tns,sn);
                            if(serviceName.equals(sqn)) {
                                hasService = true;
                            }
                            XMLStreamReaderUtil.skipElement(reader);
                        } else{
                            XMLStreamReaderUtil.skipElement(reader);
                        }
                    }

                    return new WSDLImpl(
                        rootName,systemId,src,tns,hasPortType,hasService);
                } else {
                    return new SDDocumentImpl(rootName,systemId,src);
                }
            } finally {
                reader.close();
            }
        } catch (IOException e) {
            throw new ServerRtException("runtime.parser.wsdl", systemId,e);
        } catch (XMLStreamException e) {
            throw new ServerRtException("runtime.parser.wsdl", systemId,e);
        }
    }


    private final QName rootName;
    private final SDDocumentSource source;

    /**
     * Set when {@link ServiceDefinitionImpl} is constructed.
     */
    /*package*/ ServiceDefinitionImpl owner;

    /**
     * The original system ID of this document.
     *
     * When this document contains relative references to other resources,
     * this field is used to find which {@link com.sun.xml.ws.server.SDDocumentImpl} it refers to.
     *
     * Must not be null.
     */
    private final URL url;

    protected SDDocumentImpl(QName rootName, URL url, SDDocumentSource source) {
        assert url!=null;
        this.rootName = rootName;
        this.source = source;
        this.url = url;
    }

    public QName getRootName() {
        return rootName;
    }

    public boolean isWSDL() {
        return false;
    }

    public boolean isSchema() {
        return false;
    }

    public URL getURL() {
        return url;
    }

    public XMLStreamReader read(XMLInputFactory xif) throws IOException, XMLStreamException {
        return source.read(xif);
    }

    public URL getSystemId() {
        return url;
    }

    public void writeTo(PortAddressResolver portAddressResolver, DocumentAddressResolver resolver, OutputStream os) throws IOException {
        try {
            XMLStreamWriter w = xof.createXMLStreamWriter(os);
            w.writeStartDocument();
            writeTo(portAddressResolver,resolver,w);
            w.writeEndDocument();
            w.flush();
            //w.close();
        } catch (XMLStreamException e) {
            e.printStackTrace();
            IOException ioe = new IOException(e.getMessage());
            ioe.initCause(e);
            throw ioe;
        }
    }

    public void writeTo(PortAddressResolver portAddressResolver, DocumentAddressResolver resolver, XMLStreamWriter out) throws XMLStreamException, IOException {
        new WSDLPatcher(owner.owner,this,portAddressResolver,resolver).bridge(
            source.read(xif),
            out
        );
    }


    /**
     * {@link SDDocument.Schema} implementation.
     *
     * @author Kohsuke Kawaguchi
     */
    private static final class SchemaImpl extends SDDocumentImpl implements SDDocument.Schema {
        private final String targetNamespace;

        public SchemaImpl(QName rootName, URL url, SDDocumentSource source, String targetNamespace) {
            super(rootName, url, source);
            this.targetNamespace = targetNamespace;
        }

        public String getTargetNamespace() {
            return targetNamespace;
        }

        public boolean isSchema() {
            return true;
        }
    }


    private static final class WSDLImpl extends SDDocumentImpl implements SDDocument.WSDL {
        private final String targetNamespace;
        private final boolean hasPortType;
        private final boolean hasService;

        public WSDLImpl(QName rootName, URL url, SDDocumentSource source, String targetNamespace, boolean hasPortType, boolean hasService) {
            super(rootName, url, source);
            this.targetNamespace = targetNamespace;
            this.hasPortType = hasPortType;
            this.hasService = hasService;
        }

        public String getTargetNamespace() {
            return targetNamespace;
        }

        public boolean hasPortType() {
            return hasPortType;
        }

        public boolean hasService() {
            return hasService;
        }

        public boolean isWSDL() {
            return true;
        }
    }

    private static final XMLInputFactory xif = XMLInputFactory.newInstance();
    private static final XMLOutputFactory xof = XMLOutputFactory.newInstance();
}
