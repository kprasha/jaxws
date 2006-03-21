package com.sun.xml.ws.sandbox.impl;

import com.sun.xml.stream.buffer.XMLStreamBuffer;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.sandbox.message.impl.stream.StreamHeader;
import com.sun.xml.ws.sandbox.message.impl.stream.StreamHeader11;

import javax.xml.stream.XMLStreamReader;

/**
 * {@link StreamSOAPDecoder} for SOAP 1.1.
 *
 * @author Paul.Sandoz@Sun.Com
 */
final class StreamSOAP11Decoder extends StreamSOAPDecoder {

    /*package*/  StreamSOAP11Decoder() {
        super(SOAPVersion.SOAP_11);
    }

    protected final StreamHeader createHeader(XMLStreamReader reader, XMLStreamBuffer mark) {
        return new StreamHeader11(reader, mark);
    }
}