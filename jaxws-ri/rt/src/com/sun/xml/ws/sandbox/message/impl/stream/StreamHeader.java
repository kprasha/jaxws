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
package com.sun.xml.ws.sandbox.message.impl.stream;

import com.sun.xml.bind.api.Bridge;
import com.sun.xml.bind.api.BridgeContext;
import com.sun.xml.bind.v2.util.FinalArrayList;
import com.sun.xml.stream.buffer.XMLStreamBufferException;
import com.sun.xml.stream.buffer.XMLStreamBufferMark;
import com.sun.xml.stream.buffer.XMLStreamBufferSource;
import com.sun.xml.stream.buffer.XMLStreamBuffer;
import com.sun.xml.ws.api.message.Header;
import com.sun.xml.ws.util.exception.XMLStreamException2;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.namespace.QName;
import java.util.List;


/**
 * {@link Header} whose physical data representation is an XMLStreamBuffer.
 *
 * @author Paul.Sandoz@Sun.Com
 */
public abstract class StreamHeader implements Header {

    protected static final String SOAP_1_1_MUST_UNDERSTAND = "mustUnderstand";
    protected static final String SOAP_1_2_MUST_UNDERSTAND = SOAP_1_1_MUST_UNDERSTAND;

    protected static final String SOAP_1_1_ROLE = "role";
    protected static final String SOAP_1_2_ROLE = "actor";

    protected static final String SOAP_1_2_RELAY = "relay";

    protected final XMLStreamBuffer _mark;

    protected boolean _isMustUnderstand;

    protected String _role = SOAPConstants.URI_SOAP_1_2_ROLE_ULTIMATE_RECEIVER;

    protected boolean _isRelay;

    protected String _localName;

    protected String _namespaceURI;

    /**
     * Keep the information about an attribute on the header element.
     *
     * TODO: this whole attribute handling could be done better, I think.
     */
    protected static final class Attribute {
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
    }

    /**
     * The attributes on the header element.
     * We expect there to be only a small number of them,
     * so the use of {@link List} would be justified.
     *
     * Null if no attribute is present.
     */
    private final FinalArrayList<Attribute> attributes;

    /**
     * Creates a {@link StreamHeader}.
     *
     * @param reader
     *      The parser pointing at the start of the mark.
     *      Technically this information is redundant,
     *      but it achieves a better performance.
     * @param mark
     *      The start of the buffered header content.
     */
    protected StreamHeader(XMLStreamReader reader, XMLStreamBufferMark mark) {
        assert reader!=null && mark!=null;
        _mark = mark;
        _localName = reader.getLocalName();
        _namespaceURI = reader.getNamespaceURI();
        attributes = processHeaderAttributes(reader);
    }

    /**
     * Creates a {@link StreamHeader}.
     *
     * @param reader
     *      The parser that points to the start tag of the header.
     *      By the end of this method, the parser will point at
     *      the end tag of this element.
     */
    protected StreamHeader(XMLStreamReader reader) throws XMLStreamBufferException, XMLStreamException {
        _localName = reader.getLocalName();
        _namespaceURI = reader.getNamespaceURI();
        attributes = processHeaderAttributes(reader);
        // cache the body
        _mark = new XMLStreamBuffer();
        _mark.createFromXMLStreamReader(reader);
    }

    public boolean isMustUnderstood() {
        return _isMustUnderstand;
    }

    public String getRole() {
        return _role;
    }

    public boolean isRelay() {
        return _isRelay;
    }

    public String getNamespaceURI() {
        return _namespaceURI;
    }

    public String getLocalPart() {
        return _localName;
    }

    public String getAttribute(String nsUri, String localName) {
        if(attributes!=null) {
            for(int i=attributes.size()-1; i>=0; i-- ) {
                Attribute a = attributes.get(i);
                if(a.localName.equals(localName) && a.nsUri.equals(nsUri))
                    return a.value;
            }
        }
        return null;
    }

    public String getAttribute(QName name) {
        return getAttribute(name.getNamespaceURI(),name.getLocalPart());
    }

    /**
     * Reads the header as a {@link XMLStreamReader}
     */
    public XMLStreamReader readHeader() throws XMLStreamException {
        return _mark.processUsingXMLStreamReader();
    }

    public <T> T readAsJAXB(Unmarshaller unmarshaller) throws JAXBException {
        // TODO: How can the unmarshaller process this as a fragment?
        try {
            return (T)unmarshaller.unmarshal(_mark.processUsingXMLStreamReader());
        } catch (XMLStreamException e) {
            throw new JAXBException(e);
        }
    }

    public <T> T readAsJAXB(Bridge<T> bridge, BridgeContext context) throws JAXBException {
        try {
            return bridge.unmarshal(context,_mark.processUsingXMLStreamReader());
        } catch (XMLStreamException e) {
            throw new JAXBException(e);
        }
    }

    public void writeTo(XMLStreamWriter w) throws XMLStreamException {
        try {
            // TODO what about in-scope namespaces
            _mark.processUsingXMLStreamWriter(w);
        } catch (XMLStreamBufferException e) {
            throw new XMLStreamException2(e);
        }
    }

    public void writeTo(SOAPMessage saaj) throws SOAPException {
        try {
            // TODO what about in-scope namespaces
            // Not very efficient consider implementing a stream buffer
            // processor that produces a DOM node from the buffer.
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer t = tf.newTransformer();
            XMLStreamBufferSource source = new XMLStreamBufferSource(_mark);
            DOMResult result = new DOMResult();
            t.transform(source, result);
            Node d = result.getNode();

            SOAPHeader header = saaj.getSOAPHeader();
            Node node = header.getOwnerDocument().importNode(result.getNode(), true);
            header.appendChild(node);
        } catch (Exception e) {
            throw new SOAPException(e);
        }
    }

    public void writeTo(ContentHandler contentHandler, ErrorHandler errorHandler) throws SAXException {
        try {
            _mark.processUsingSAXContentHandler(contentHandler);
        } catch (XMLStreamBufferException e) {
            throw new SAXException(e);
        }
    }

    protected abstract FinalArrayList<Attribute> processHeaderAttributes(XMLStreamReader reader);

    /**
     * Convert null to "".
     */
    private static String fixNull(String s) {
        if(s==null) return "";
        else        return s;
    }
}
