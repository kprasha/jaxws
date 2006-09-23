package com.sun.xml.ws.api.addressing;

import com.sun.istack.FinalArrayList;
import com.sun.istack.NotNull;
import com.sun.xml.stream.buffer.XMLStreamBuffer;
import com.sun.xml.stream.buffer.XMLStreamBufferException;
import com.sun.xml.ws.api.message.Header;
import com.sun.xml.ws.message.AbstractHeaderImpl;
import com.sun.xml.ws.util.xml.XMLStreamWriterFilter;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.XMLFilterImpl;

import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.ws.WebServiceException;

/**
 * Used to represent outbound header created from {@link WSEndpointReference}'s
 * referenec parameters.
 *
 * <p>
 * This is optimized for outbound use, so it implements some of the methods lazily,
 * in a slow way.
 *
 * <p>
 * This header adds "wsa:IsReferenceParameter" and thus only used for the W3C version.
 *
 * @author Kohsuke Kawaguchi
 */
final class OutboundReferenceParameterHeader extends AbstractHeaderImpl {
    private final XMLStreamBuffer infoset;
    private final String nsUri,localName;

    /**
     * The attributes on the header element.
     * Lazily parsed.
     * Null if not parsed yet.
     */
    private FinalArrayList<Attribute> attributes;

    OutboundReferenceParameterHeader(XMLStreamBuffer infoset, String nsUri, String localName) {
        this.infoset = infoset;
        this.nsUri = nsUri;
        this.localName = localName;
    }

    public @NotNull String getNamespaceURI() {
        return nsUri;
    }

    public @NotNull String getLocalPart() {
        return localName;
    }

    public String getAttribute(String nsUri, String localName) {
        if(attributes==null)
            parseAttributes();
        for(int i=attributes.size()-1; i>=0; i-- ) {
            Attribute a = attributes.get(i);
            if(a.localName.equals(localName) && a.nsUri.equals(nsUri))
                return a.value;
        }
        return null;
    }

    /**
     * We don't really expect this to be used, but just to satisfy
     * the {@link Header} contract.
     *
     * So this is rather slow.
     */
    private void parseAttributes() {
        try {
            XMLStreamReader reader = readHeader();

            attributes = new FinalArrayList<Attribute>();

            for (int i = 0; i < reader.getAttributeCount(); i++) {
                final String localName = reader.getAttributeLocalName(i);
                final String namespaceURI = reader.getAttributeNamespace(i);
                final String value = reader.getAttributeValue(i);

                attributes.add(new Attribute(namespaceURI,localName,value));
            }

            // we are adding one more attribute "wsa:IsReferenceParameter"
            attributes.add(new Attribute(AddressingVersion.W3C.nsUri,"IsReferenceParameter",TRUE_VALUE));
        } catch (XMLStreamException e) {
            throw new WebServiceException("Unable to read the attributes for {"+nsUri+"}"+localName+" header",e);
        }
    }

    public XMLStreamReader readHeader() throws XMLStreamException {
        // TODO: implement this method later
        throw new UnsupportedOperationException();
    }

    public void writeTo(XMLStreamWriter w) throws XMLStreamException {
        try {
            infoset.writeToXMLStreamWriter(new XMLStreamWriterFilter(w) {
                private boolean root=true;

                public void writeStartElement(String localName) throws XMLStreamException {
                    super.writeStartElement(localName);
                    writeAddedAttribute();
                }

                private void writeAddedAttribute() throws XMLStreamException {
                    if(!root)   return;
                    root=true;
                    super.writeNamespace("wsa",AddressingVersion.W3C.nsUri);
                    super.writeAttribute("wsa",AddressingVersion.W3C.nsUri,"IsReferenceParameter",TRUE_VALUE);
                }

                public void writeStartElement(String namespaceURI, String localName) throws XMLStreamException {
                    super.writeStartElement(namespaceURI, localName);
                    writeAddedAttribute();
                }

                public void writeStartElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
                    super.writeStartElement(prefix, localName, namespaceURI);
                    writeAddedAttribute();
                }
            });
        } catch (XMLStreamBufferException e) {
            throw new XMLStreamException(e);
        }
    }

    public void writeTo(SOAPMessage saaj) throws SOAPException {
        try {
            Element node = (Element)infoset.writeTo(saaj.getSOAPHeader());
            node.setAttributeNS(AddressingVersion.W3C.nsUri,"IsReferenceParameter",TRUE_VALUE);
        } catch (XMLStreamBufferException e) {
            throw new SOAPException(e);
        }
    }

    public void writeTo(ContentHandler contentHandler, ErrorHandler errorHandler) throws SAXException {
        class Filter extends XMLFilterImpl {
            Filter(ContentHandler ch) { setContentHandler(ch); }
            private int depth=0;
            public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
                if(depth++==0) {
                    // add one more attribute
                    super.startPrefixMapping("wsa",AddressingVersion.W3C.nsUri);
                    AttributesImpl atts2 = new AttributesImpl(atts);
                    atts2.addAttribute(
                        AddressingVersion.W3C.nsUri,
                        "IsReferenceParameter",
                        "wsa:IsReferenceParameter",
                        "CDATA",
                        TRUE_VALUE);
                    atts = atts2;
                }

                super.startElement(uri, localName, qName, atts);
            }

            public void endElement(String uri, String localName, String qName) throws SAXException {
                super.endElement(uri, localName, qName);
                if(--depth==0)
                    super.endPrefixMapping("wsa");
            }
        }

        infoset.writeTo(new Filter(contentHandler),errorHandler);
    }


    /**
     * Keep the information about an attribute on the header element.
     */
    static final class Attribute {
        /**
         * Can be empty but never null.
         */
        final String nsUri;
        final String localName;
        final String value;

        public Attribute(String nsUri, String localName, String value) {
            this.nsUri = fixNull(nsUri);
            this.localName = localName;
            this.value = value;
        }

        /**
         * Convert null to "".
         */
        private static String fixNull(String s) {
            if(s==null) return "";
            else        return s;
        }
    }

    /**
     * We the performance paranoid people in the JAX-WS RI thinks
     * saving three bytes is worth while...
     */
    private static final String TRUE_VALUE = "1";
}
