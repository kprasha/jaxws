package com.sun.xml.ws.api.addressing;

import com.sun.istack.FinalArrayList;
import com.sun.istack.NotNull;
import com.sun.xml.stream.buffer.XMLStreamBuffer;
import com.sun.xml.stream.buffer.XMLStreamBufferException;
import com.sun.xml.ws.api.message.Header;
import com.sun.xml.ws.message.AbstractHeaderImpl;
import org.w3c.dom.Element;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

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
        } catch (XMLStreamException e) {
            throw new WebServiceException("Unable to read the attributes for {"+nsUri+"}"+localName+" header",e);
        }
    }

    public XMLStreamReader readHeader() throws XMLStreamException {
        // TODO: implement this method later
        throw new UnsupportedOperationException();
    }

    public void writeTo(XMLStreamWriter w) throws XMLStreamException {
        // TODO: implement this method later
        throw new UnsupportedOperationException();
    }

    public void writeTo(SOAPMessage saaj) throws SOAPException {
        try {
            Element node = (Element)infoset.writeTo(saaj.getSOAPHeader());
            node.setAttributeNS(AddressingVersion.W3C.nsUri,"IsReferenceParameter","1");
        } catch (XMLStreamBufferException e) {
            throw new SOAPException(e);
        }
    }

    public void writeTo(ContentHandler contentHandler, ErrorHandler errorHandler) throws SAXException {
        throw new UnsupportedOperationException();
    }


    /**
     * Keep the information about an attribute on the header element.
     *
     * TODO: this whole attribute handling could be done better, I think.
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
}
