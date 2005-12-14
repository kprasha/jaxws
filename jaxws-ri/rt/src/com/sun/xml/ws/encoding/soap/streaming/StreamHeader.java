package com.sun.xml.ws.encoding.soap.streaming;

import com.sun.xml.stream.buffer.XMLStreamBufferMark;
import com.sun.xml.ws.sandbox.XMLStreamWriterEx;
import com.sun.xml.ws.sandbox.message.Header;
import javax.xml.bind.Unmarshaller;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 *
 * @author Paul.Sandoz@Sun.Com
 */
public class StreamHeader implements Header {
    private static final String SOAP_NAMESPACE_URI = "http://....";

    private static final String SOAP_MUST_UNDERSTAND = "mustUnderstand";
    private static final String SOAP_ROLE = "role";
    private static final String SOAP_RELAY = "relay";
  
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
    
    private void processHeaderAttributes(XMLStreamReader reader) {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            final String localName = reader.getAttributeLocalName(i);
            final String namespaceURI = reader.getAttributeNamespace(i);

            if (namespaceURI == SOAP_NAMESPACE_URI) {
                if (localName == SOAP_MUST_UNDERSTAND) {
                    final String value = reader.getAttributeValue(i);
                    if (value != null && (value.equals("1") || value.equals("true"))) {
                        _isMustUnderstand = true;
                    }
                } else if (localName == SOAP_ROLE) {
                    final String value = reader.getAttributeValue(i);
                    if (value != null && value.length() > 0) {
                        _role = value;
                    }
                } else if (localName == SOAP_RELAY) {
                    final String value = reader.getAttributeValue(i);
                    if (value != null && (value.equals("1") || value.equals("true"))) {
                        _isRelay = true;
                    }
                }
            }
        }
    }
}
