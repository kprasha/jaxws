package com.sun.xml.ws.encoding.fastinfoset;

import com.sun.xml.stream.buffer.XMLStreamBuffer;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.sandbox.message.impl.stream.StreamHeader;
import com.sun.xml.ws.sandbox.message.impl.stream.StreamHeader12;

import javax.xml.stream.XMLStreamReader;

/**
 * A decoder that decodes SOAP 1.2 messages infosets encoded as fast infoset
 * documents.
 *
 * @author Paul.Sandoz@Sun.Com
 */
final class FastInfosetStreamSOAP12Decoder extends FastInfosetStreamSOAPDecoder{

    /*package*/ FastInfosetStreamSOAP12Decoder() {
        super(SOAPVersion.SOAP_12);
    }

    protected final StreamHeader createHeader(XMLStreamReader reader, XMLStreamBuffer mark) {
        return new StreamHeader12(reader, mark);
    }
}