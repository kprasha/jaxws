package com.sun.xml.ws.api.addressing;

import com.sun.istack.NotNull;
import com.sun.xml.stream.buffer.MutableXMLStreamBuffer;
import com.sun.xml.stream.buffer.XMLStreamBuffer;
import com.sun.xml.stream.buffer.XMLStreamBufferException;
import com.sun.xml.stream.buffer.XMLStreamBufferResult;
import com.sun.xml.stream.buffer.XMLStreamBufferSource;
import com.sun.xml.stream.buffer.sax.SAXBufferProcessor;
import com.sun.xml.stream.buffer.stax.StreamReaderBufferProcessor;
import com.sun.xml.ws.api.message.Header;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.spi.ProviderImpl;
import com.sun.xml.ws.util.xml.XMLStreamWriterFilter;
import com.sun.xml.ws.util.xml.XmlUtil;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.ws.EndpointReference;
import javax.xml.ws.WebServiceException;
import java.io.StringWriter;

/**
 * Internal representation of the EPR.
 *
 * <p>
 * Instances of this class are immutable and thread-safe.
 *
 * @author Kohsuke Kawaguchi
 */
public final class WSEndpointReference {
    private final XMLStreamBuffer infoset;

    /**
     * Creates from the spec version of {@link EndpointReference}.
     *
     * <p>
     * This method performs the data conversion, so it's slow.
     * Do not use this method in a performance critical path.
     */
    public WSEndpointReference(EndpointReference epr) {
        MutableXMLStreamBuffer xsb = new MutableXMLStreamBuffer();
        epr.writeTo(new XMLStreamBufferResult(xsb));
        this.infoset = xsb;
    }

    /**
     * Creates a {@link WSEndpointReference} that wraps a given infoset.
     */
    public WSEndpointReference(XMLStreamBuffer infoset) {
        this.infoset = infoset;
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
        p.process();
    }

    /**
     * Writes this EPR into the given writer.
     *
     * @param localName
     *      EPR uses a different root tag name depending on the context.
     *      The returned {@link Source} will use the given local name
     */
    public void writeTo(final @NotNull String localName, @NotNull XMLStreamWriter w) throws XMLStreamBufferException, XMLStreamException {
        infoset.writeToXMLStreamWriter(new XMLStreamWriterFilter(w) {
            private boolean root=true;

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
    public Header asHeader(QName rootTagName) {
        return new EPRHeader(rootTagName,this);
    }

    /**
     * Dumps the EPR infoset in a human-readable string.
     */
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
}
