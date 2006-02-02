package com.sun.xml.ws.sandbox.impl;

import com.sun.xml.stream.buffer.XMLStreamBufferMark;
import com.sun.xml.ws.sandbox.message.impl.stream.StreamHeader;
import com.sun.xml.ws.sandbox.message.impl.stream.StreamHeader11;
import com.sun.xml.ws.sandbox.message.impl.stream.StreamMessage;
import com.sun.xml.ws.api.message.HeaderList;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.pipe.Decoder;

import javax.xml.soap.SOAPConstants;
import javax.xml.stream.XMLStreamReader;

/**
 * {@link StreamSOAPDecoder} for SOAP 1.1.
 *
 * @author Paul.Sandoz@Sun.Com
 */
final class StreamSOAP11Decoder extends StreamSOAPDecoder{
    
    public StreamSOAP11Decoder() {
        super(SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE);
    }
    
    protected final StreamHeader createHeader(XMLStreamReader reader, XMLStreamBufferMark mark) {
        return new StreamHeader11(reader, mark);
    }

    protected StreamMessage createMessage(HeaderList headers, XMLStreamReader reader) {
        return new StreamMessage(headers, reader, SOAPVersion.fromNsUri(SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE));
    }
}