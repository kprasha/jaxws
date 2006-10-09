package com.sun.xml.ws.api.addressing;

import com.sun.istack.NotNull;
import com.sun.xml.stream.buffer.MutableXMLStreamBuffer;
import com.sun.xml.stream.buffer.XMLStreamBuffer;
import com.sun.xml.stream.buffer.XMLStreamBufferResult;
import com.sun.xml.stream.buffer.XMLStreamBufferSource;
import com.sun.xml.stream.buffer.sax.SAXBufferProcessor;
import com.sun.xml.stream.buffer.stax.StreamReaderBufferProcessor;
import com.sun.xml.ws.addressing.EndpointReferenceUtil;
import com.sun.xml.ws.addressing.model.InvalidMapException;
import com.sun.xml.ws.api.message.Header;
import com.sun.xml.ws.api.message.HeaderList;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.resources.AddressingMessages;
import com.sun.xml.ws.spi.ProviderImpl;
import com.sun.xml.ws.streaming.XMLStreamReaderUtil;
import com.sun.xml.ws.util.xml.XMLStreamWriterFilter;
import com.sun.xml.ws.util.xml.XmlUtil;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.ws.Dispatch;
import javax.xml.ws.EndpointReference;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Internal representation of the EPR.
 *
 * <p>
 * Instances of this class are immutable and thread-safe.
 *
 * @author Kohsuke Kawaguchi
 * @see AddressingVersion#anonymousEpr
 */
public final class WSEndpointReference {
    private final XMLStreamBuffer infoset;
    /**
     * Version of the addressing spec.
     */
    private final AddressingVersion version;

    /**
     * Marked Reference parameters inside this EPR.
     *
     * Parsed when the object is created. can be empty but never null.
     * @see #parse()
     */
    private @NotNull Header[] referenceParameters;
    private String address;

    /**
     * Creates from the spec version of {@link EndpointReference}.
     *
     * <p>
     * This method performs the data conversion, so it's slow.
     * Do not use this method in a performance critical path.
     */
    public WSEndpointReference(EndpointReference epr, AddressingVersion version) throws XMLStreamException {
        MutableXMLStreamBuffer xsb = new MutableXMLStreamBuffer();
        epr.writeTo(new XMLStreamBufferResult(xsb));
        this.infoset = xsb;
        this.version = version;
        parse();
    }

    /**
     * Creates a {@link WSEndpointReference} that wraps a given infoset.
     */
    public WSEndpointReference(XMLStreamBuffer infoset, AddressingVersion version) throws XMLStreamException {
        this.infoset = infoset;
        this.version = version;
        parse();
    }

    /**
     * Creates a {@link WSEndpointReference} by parsing an infoset.
     */
    public WSEndpointReference(InputStream infoset, AddressingVersion version) throws XMLStreamException {
        this(XMLInputFactory.newInstance().createXMLStreamReader(infoset),version);
    }

    /**
     * Creates a {@link WSEndpointReference} from the given infoset.
     * The {@link XMLStreamReader} must point to either a document or an element.
     */
    public WSEndpointReference(XMLStreamReader in, AddressingVersion version) throws XMLStreamException {
        this(XMLStreamBuffer.createNewBufferFromXMLStreamReader(in), version);
    }

    /**
     * Convert the EPR to the spec version. The actual type of
     * {@link EndpointReference} to be returned depends on which version
     * of the addressing spec this EPR conforms to.
     *
     * @throws WebServiceException
     *      if the conversion fails, which can happen if the EPR contains
     *      invalid infoset (wrong namespace URI, etc.)
     */
    public @NotNull EndpointReference toSpec() {
        return ProviderImpl.INSTANCE.readEndpointReference(new XMLStreamBufferSource(infoset));
    }

    /**
     * Converts the EPR to the specified spec version.
     *
     * If the {@link #getVersion() the addressing version in use} and
     * the given class is different, then this may involve version conversion. 
     */
    public @NotNull <T extends EndpointReference> T toSpec(Class<T> clazz) {
        return EndpointReferenceUtil.transform(clazz,toSpec());
    }

    /**
     * Creates a proxy that can be used to talk to this EPR.
     *
     * <p>
     * All the normal WS-Addressing processing happens automatically,
     * such as setting the endpoint address to {@link #getAddress() the address},
     * and sending the reference parameters associated with this EPR as
     * headers, etc.
     */
    public @NotNull <T> T getPort(@NotNull Service jaxwsService,
                     @NotNull Class<T> serviceEndpointInterface,
                     WebServiceFeature... features)     {
        // TODO: implement it in a better way
        return jaxwsService.getPort(toSpec(),serviceEndpointInterface,features);
    }

    /**
     * Creates a {@link Dispatch} that can be used to talk to this EPR.
     *
     * <p>
     * All the normal WS-Addressing processing happens automatically,
     * such as setting the endpoint address to {@link #getAddress() the address},
     * and sending the reference parameters associated with this EPR as
     * headers, etc.
     */
    public @NotNull <T> Dispatch<T> createDispatch(
        @NotNull Service jaxwsService,
        @NotNull Class<T> type,
        @NotNull Service.Mode mode,
        WebServiceFeature... features) {

        // TODO: implement it in a better way
        return jaxwsService.createDispatch(toSpec(),type,mode,features);
    }

    /**
     * Creates a {@link Dispatch} that can be used to talk to this EPR.
     *
     * <p>
     * All the normal WS-Addressing processing happens automatically,
     * such as setting the endpoint address to {@link #getAddress() the address},
     * and sending the reference parameters associated with this EPR as
     * headers, etc.
     */
    public @NotNull Dispatch<Object> createDispatch(
        @NotNull Service jaxwsService,
        @NotNull JAXBContext context,
        @NotNull Service.Mode mode,
        WebServiceFeature... features) {

        // TODO: implement it in a better way
        return jaxwsService.createDispatch(toSpec(),context,mode,features);
    }

    /**
     * Gets the addressing version of this EPR.
     */
    public @NotNull AddressingVersion getVersion() {
        return version;
    }

    /**
     * The value of the &lt;wsa:address> header.
     */
    public @NotNull String getAddress() {
        return address;
    }

    /**
     * Parses inside EPR and mark all reference parameters.
     */
    private void parse() throws XMLStreamException {
        // TODO: validate the EPR structure.
        // check for non-existent Address, that sort of things.

        StreamReaderBufferProcessor xsr = infoset.readAsXMLStreamReader();

        // parser should be either at the start element or the start document
        if(xsr.getEventType()==XMLStreamReader.START_DOCUMENT)
            xsr.nextTag();
        assert xsr.getEventType()==XMLStreamReader.START_ELEMENT;

        String rootLocalName = xsr.getLocalName();
        if(!xsr.getNamespaceURI().equals(version.nsUri))
            throw new WebServiceException(AddressingMessages.WRONG_ADDRESSING_VERSION(
                version.nsUri, xsr.getNamespaceURI()));

        // since often EPR doesn't have a reference parameter, create array lazily
        List<Header> marks=null;

        while(xsr.nextTag()==XMLStreamReader.START_ELEMENT) {
            String localName = xsr.getLocalName();
            if(version.isReferenceParameter(localName)) {
                XMLStreamBuffer mark;
                while((mark = xsr.nextTagAndMark())!=null) {
                    if(marks==null)
                        marks = new ArrayList<Header>();

                    // TODO: need a different header for member submission version
                    marks.add(version.createReferenceParameterHeader(
                        mark, xsr.getNamespaceURI(), xsr.getLocalName()));
                    XMLStreamReaderUtil.skipElement(xsr);
                }
            } else
            if(localName.equals("Address")) {
                if(address!=null) // double <Address>. That's an error.
                    throw new InvalidMapException(new QName(version.nsUri,rootLocalName),AddressingVersion.fault_duplicateAddressInEpr);
                address = xsr.getElementText();
            } else {
                XMLStreamReaderUtil.skipElement(xsr);
            }
        }

        // hit to </EndpointReference> by now

        if(marks==null) {
            this.referenceParameters = EMPTY_ARRAY;
        } else {
            this.referenceParameters = marks.toArray(new Header[marks.size()]);
        }

        if(address==null)
            throw new InvalidMapException(new QName(version.nsUri,rootLocalName),version.fault_missingAddressInEpr);
    }

    /**
     * Reads this EPR as {@link XMLStreamReader}.
     *
     * @param localName
     *      EPR uses a different root tag name depending on the context.
     *      The returned {@link XMLStreamReader} will use the given local name
     *      for the root element name.
     */
    public XMLStreamReader read(final @NotNull String localName) throws XMLStreamException {
        return new StreamReaderBufferProcessor(infoset) {
            protected void processElement(String prefix, String uri, String _localName) {
                if(_depth==0)
                        _localName = localName;
                super.processElement(prefix, uri, _localName);
            }
        };
    }

    /**
     * Returns a {@link Source} that represents this EPR.
     *
     * @param localName
     *      EPR uses a different root tag name depending on the context.
     *      The returned {@link Source} will use the given local name
     *      for the root element name.
     */
    public Source asSource(@NotNull String localName) {
        return new SAXSource(new SAXBufferProcessorImpl(localName),new InputSource());
    }

    /**
     * Writes this EPR to the given {@link ContentHandler}.
     *
     * @param localName
     *      EPR uses a different root tag name depending on the context.
     *      The returned {@link Source} will use the given local name
     *      for the root element name.
     */
    public void writeTo(@NotNull String localName, ContentHandler contentHandler, ErrorHandler errorHandler) throws SAXException {
        SAXBufferProcessorImpl p = new SAXBufferProcessorImpl(localName);
        p.setContentHandler(contentHandler);
        p.setErrorHandler(errorHandler);
        p.process(infoset);
    }

    /**
     * Writes this EPR into the given writer.
     *
     * @param localName
     *      EPR uses a different root tag name depending on the context.
     *      The returned {@link Source} will use the given local name
     */
    public void writeTo(final @NotNull String localName, @NotNull XMLStreamWriter w) throws XMLStreamException {
        infoset.writeToXMLStreamWriter(new XMLStreamWriterFilter(w) {
            private boolean root=true;

            @Override
            public void writeStartDocument() throws XMLStreamException {
            }

            @Override
            public void writeStartDocument(String encoding, String version) throws XMLStreamException {
            }

            @Override
            public void writeStartDocument(String version) throws XMLStreamException {
            }

            @Override
            public void writeEndDocument() throws XMLStreamException {
            }

            private String override(String ln) {
                if(root) {
                    root = false;
                    return localName;
                }
                return ln;
            }

            public void writeStartElement(String localName) throws XMLStreamException {
                super.writeStartElement(override(localName));
            }

            public void writeStartElement(String namespaceURI, String localName) throws XMLStreamException {
                super.writeStartElement(namespaceURI, override(localName));
            }

            public void writeStartElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
                super.writeStartElement(prefix, override(localName), namespaceURI);
            }
        });
    }

    /**
     * Returns a {@link Header} that wraps this {@link WSEndpointReference}.
     *
     * <p>
     * The returned header is immutable too, and can be reused with
     * many {@link Message}s.
     *
     * @param rootTagName
     *      The header tag name to be used, such as &lt;ReplyTo> or &lt;FaultTo>.
     *      (It's bit ugly that this method takes {@link QName} and not just local name,
     *      unlike other methods. If it's making the caller's life miserable, then
     *      we can talk.)
     */
    public Header createHeader(QName rootTagName) {
        return new EPRHeader(rootTagName,this);
    }

    /**
     * Copies all the reference parameters in this EPR as headers
     * to the given {@link HeaderList}.
     */
    public void addReferenceParameters(HeaderList outbound) {
        for (Header header : referenceParameters) {
            outbound.add(header);
        }
    }

    /**
     * Dumps the EPR infoset in a human-readable string.
     */
    @Override
    public String toString() {
        try {
            // debug convenience
            StringWriter sw = new StringWriter();
            XmlUtil.newTransformer().transform(asSource("EndpointReference"),new StreamResult(sw));
            return sw.toString();
        } catch (TransformerException e) {
            return e.toString();
        }
    }

    /**
     * Filtering {@link SAXBufferProcessor} that replaces the root tag name.
     */
    class SAXBufferProcessorImpl extends SAXBufferProcessor {
        private final String rootLocalName;
        private boolean root=true;

        public SAXBufferProcessorImpl(String rootLocalName) {
            super(infoset);
            this.rootLocalName = rootLocalName;
        }

        protected void processElement(String uri, String localName, String qName) throws SAXException {
            if(root) {
                root = false;

                if(qName.equals(localName)) {
                    qName = localName = rootLocalName;
                } else {
                    localName = rootLocalName;
                    int idx = qName.indexOf(':');
                    qName = qName.substring(0,idx+1)+rootLocalName;
                }
            }
            super.processElement(uri, localName, qName);
        }
    }

    private static final OutboundReferenceParameterHeader[] EMPTY_ARRAY = new OutboundReferenceParameterHeader[0];
}
