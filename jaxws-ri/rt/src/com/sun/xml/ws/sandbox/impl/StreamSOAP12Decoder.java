package com.sun.xml.ws.sandbox.impl;

import com.sun.xml.stream.buffer.XMLStreamBufferMark;
import com.sun.xml.ws.sandbox.message.impl.stream.StreamHeader;
import com.sun.xml.ws.sandbox.message.impl.stream.StreamHeader12;
import com.sun.xml.ws.sandbox.message.impl.stream.StreamMessage;
import com.sun.xml.ws.api.message.HeaderList;
import com.sun.xml.ws.api.SOAPVersion;

import javax.xml.soap.SOAPConstants;
import javax.xml.stream.XMLStreamReader;

/**
 * {@link StreamSOAPDecoder} for SOAP 1.2.
 *
 * @author Paul.Sandoz@Sun.Com
 */
public class StreamSOAP12Decoder extends StreamSOAPDecoder{
    
    public StreamSOAP12Decoder() {
        super(SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE);
    }
    
    protected final StreamHeader createHeader(XMLStreamReader reader, XMLStreamBufferMark mark) {
        return new StreamHeader12(reader, mark);
    }

    protected StreamMessage createMessage(HeaderList headers, XMLStreamReader reader) {
        return new StreamMessage(headers, reader, SOAPVersion.fromNsUri(SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE));
    }

}