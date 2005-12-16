package com.sun.xml.ws.sandbox.message.impl.stream;

import com.sun.xml.stream.buffer.XMLStreamBufferMark;
import com.sun.xml.ws.sandbox.XMLStreamWriterEx;
import com.sun.xml.ws.sandbox.message.Header;
import javax.xml.bind.Unmarshaller;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;


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

    protected final XMLStreamBufferMark _mark;

    protected boolean _isMustUnderstand;

    protected String _role = SOAPConstants.URI_SOAP_1_2_ROLE_ULTIMATE_RECEIVER;

    protected boolean _isRelay;

    protected String _localName;

    protected String _namespaceURI;

    public StreamHeader(XMLStreamReader reader, XMLStreamBufferMark mark) {
        _mark = mark;
        _localName = reader.getLocalName();
        _namespaceURI = reader.getNamespaceURI();

        processHeaderAttributes(reader);
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

    /**
     * Reads the header as a {@link XMLStreamReader}
     */
    public XMLStreamReader readHeader() {
        throw new UnsupportedOperationException();
    }

    public <T> T readAsJAXB(Unmarshaller unmarshaller) {
        throw new UnsupportedOperationException();
    }

    public void writeTo(XMLStreamWriterEx w) throws XMLStreamException {
        throw new UnsupportedOperationException();
    }

    public void writeTo(SOAPMessage saaj) {
        throw new UnsupportedOperationException();
    }

    public void writeTo(ContentHandler contentHandler, ErrorHandler errorHandler) throws SAXException {
        throw new UnsupportedOperationException();
    }

    protected abstract void processHeaderAttributes(XMLStreamReader reader);

    protected final boolean convertToBoolean(String value) {
        if (value != null && (value.equals("1") || value.equals("true"))) {
            return true;
        } else {
            return false;
        }
    }
}
