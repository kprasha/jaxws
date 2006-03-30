package com.sun.xml.ws.encoding.fastinfoset;

import com.sun.xml.fastinfoset.stax.StAXDocumentParser;
import com.sun.xml.stream.buffer.XMLStreamBuffer;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.Decoder;
import com.sun.xml.ws.sandbox.impl.StreamSOAPDecoder;
import com.sun.xml.ws.sandbox.message.impl.stream.StreamHeader;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;

/**
 * A stream SOAP decoder for decoding SOAP message infosets encoded as fast
 * infoset documents.
 * <p>
 * This implementation currently defers to {@link StreamSOAPDecoder} for the decoding
 * using {@link XMLStreamReader}.
 *
 * @author Paul Sandoz
 */
public abstract class FastInfosetStreamSOAPDecoder implements Decoder {
    private StreamSOAPDecoder _soapDecoder;
    
    /* package */ FastInfosetStreamSOAPDecoder(SOAPVersion soapVersion) {
        _soapDecoder = StreamSOAPDecoder.create(soapVersion);
    }

    public void decode(InputStream in, String contentType, Packet response) throws IOException {
        XMLStreamReader reader = createXMLStreamReader(in);
        response.setMessage(_soapDecoder.decode(reader));
    }

    public void decode(ReadableByteChannel in, String contentType, Packet response) {
        throw new UnsupportedOperationException();
    }

    public Decoder copy() {
        // TODO: when you make Decoder stateful, implement the copy method.
        // This also depends on the underlying SOAP decoder being stateless
        return this;
    }
    
    protected XMLStreamReader createXMLStreamReader(InputStream in) {
        // TODO: we should definitely let Decode owns one XMLStreamReader instance
        // instead of instantiating a new parser
        return new StAXDocumentParser(in);
    }

    protected abstract StreamHeader createHeader(XMLStreamReader reader, XMLStreamBuffer mark);
    
    /**
     * Creates a new {@link FastInfosetStreamSOAPDecoder} instance.
     */
    public static FastInfosetStreamSOAPDecoder create(SOAPVersion version) {
        if(version==null)
            // this decoder is for SOAP, not for XML/HTTP
            throw new IllegalArgumentException();
        switch(version) {
            case SOAP_11:
                return new FastInfosetStreamSOAP11Decoder();
            case SOAP_12:
                return new FastInfosetStreamSOAP12Decoder();
            default:
                throw new AssertionError();
        }
    }

}
