package com.sun.xml.ws.sandbox.impl;

import com.sun.xml.stream.buffer.XMLStreamBufferMark;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.sandbox.message.impl.stream.StreamHeader;
import com.sun.xml.ws.sandbox.message.impl.stream.StreamHeader12;

import javax.xml.stream.XMLStreamReader;

/**
 * {@link StreamSOAPDecoder} for SOAP 1.2.
 *
 * @author Paul.Sandoz@Sun.Com
 */
final class StreamSOAP12Decoder extends StreamSOAPDecoder{

    /*package*/ StreamSOAP12Decoder() {
        super(SOAPVersion.SOAP_12);
    }

    protected final StreamHeader createHeader(XMLStreamReader reader, XMLStreamBufferMark mark) {
        return new StreamHeader12(reader, mark);
    }
}