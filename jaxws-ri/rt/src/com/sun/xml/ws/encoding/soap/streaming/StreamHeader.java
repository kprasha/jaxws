package com.sun.xml.ws.encoding.soap.streaming;

import com.sun.xml.ws.sandbox.XMLStreamWriterEx;
import com.sun.xml.ws.sandbox.message.Header;
import javax.xml.bind.Unmarshaller;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 *
 * @author Paul.Sandoz@Sun.Com
 */
public class StreamHeader implements Header {
    
    protected final boolean _isMustUnderstand;
    
    protected final String _role;
    
    protected final boolean _isRelay;
    
    protected final String _localName;
    
    protected final String _namespaceURI;
    
    public StreamHeader() {
        _isMustUnderstand = false;
        _role = "http://www.w3.org/2003/05/soap-envelope/role/ultimateReceiver";
        _isRelay = false;
        _localName = "";
        _namespaceURI = "";
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
}
