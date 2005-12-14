package com.sun.xml.ws.encoding.soap.streaming;

import com.sun.xml.ws.sandbox.XMLStreamWriterEx;
import com.sun.xml.ws.sandbox.message.AttachmentSet;
import com.sun.xml.ws.sandbox.message.HeaderList;
import com.sun.xml.ws.sandbox.message.Message;
import com.sun.xml.ws.sandbox.message.MessageProperties;
import javax.xml.bind.Unmarshaller;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;

public class StreamMessage extends Message {
    /*
     * The reader will be positioned at
     * the first child of the SOAP body
     */
    protected XMLStreamReader _reader;
    
    protected String _payloadLocalName;

    protected String _payloadNamespaceURI;

    private final HeaderList headers;
    
    public StreamMessage(HeaderList headers, XMLStreamReader reader) {
        this.headers = headers;
        
        _reader = reader;
        
        _payloadLocalName = _reader.getLocalName();
        _payloadNamespaceURI = _reader.getNamespaceURI();
    }
    
    public StreamMessage(HeaderList headers) {
        this.headers = headers;

        _reader = null;
        
        _payloadLocalName = "";
        _payloadNamespaceURI = "";
    }

    public HeaderList getHeaders() {
        return headers;
    }

    public MessageProperties getProperties() {
        throw new UnsupportedOperationException();
    }

    public AttachmentSet getAttachments() {
        throw new UnsupportedOperationException();
    }

    public String getPayloadLocalPart() {
        return _payloadLocalName;
    }

    public String getPayloadNamespaceURI() {
        return _payloadNamespaceURI;
    }

    public boolean isFault() {
        throw new UnsupportedOperationException();
    }

    public Source readEnvelopeAsSource() {
        throw new UnsupportedOperationException();
    }

    public Source readPayloadAsSource() {
        throw new UnsupportedOperationException();
    }

    public SOAPMessage readAsSOAPMessage() {
        throw new UnsupportedOperationException();
    }

    public <T> T readAsJAXB(Unmarshaller unmarshaller) {
        throw new UnsupportedOperationException();
    }

    public XMLStreamReader readPayload() {
        return _reader;
    }

    
    
    public void writePayloadTo(XMLStreamWriterEx sw) {
        throw new UnsupportedOperationException();
    }

    public void writeTo(XMLStreamWriterEx sw) {
        throw new UnsupportedOperationException();
    }

    public Message copy() {
        throw new UnsupportedOperationException();
    }
}
