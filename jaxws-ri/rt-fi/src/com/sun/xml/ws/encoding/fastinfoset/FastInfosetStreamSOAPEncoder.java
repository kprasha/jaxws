package com.sun.xml.ws.encoding.fastinfoset;

import com.sun.xml.fastinfoset.stax.StAXDocumentSerializer;
import com.sun.xml.ws.api.pipe.Encoder;
import com.sun.xml.ws.api.pipe.ContentType;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.SOAPVersion;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.ws.WebServiceException;
import java.io.OutputStream;
import java.nio.channels.WritableByteChannel;

/**
 * A stream SOAP encoder for encoding SOAP message infosets to fast
 * infoset documents.
 *
 * @author Paul Sandoz
 */
public abstract class FastInfosetStreamSOAPEncoder implements Encoder {
    
    public ContentType getStaticContentType(Packet packet) {
        return getContentType(packet.soapAction);
    }

    public ContentType encode(Packet packet, OutputStream out) {
        if (packet.getMessage() != null) {
            XMLStreamWriter writer = createXMLStreamWriter(out);
            try {
                packet.getMessage().writeTo(writer);
                writer.flush();
            } catch (XMLStreamException e) {
                throw new WebServiceException(e);
            }
        }
        return getContentType(packet.soapAction);
    }

    public ContentType encode(Packet packet, WritableByteChannel buffer) {
        //TODO: not yet implemented
        throw new UnsupportedOperationException();
    }

    public Encoder copy() {
        // TODO: when you make Decoder stateful, implement the copy method.
        // This also depends on the underlying SOAP decoder being stateless
        return this;
    }

    protected XMLStreamWriter createXMLStreamWriter(OutputStream out) {
        // TODO: we should definitely let Encode owns one XMLStreamWriter instance
        // instead of instantiating a new one
        return new StAXDocumentSerializer(out);
    }
    
    protected abstract ContentType getContentType(String soapAction);
    
    public static FastInfosetStreamSOAPEncoder get(SOAPVersion version) {
        if(version==null)
            // this decoder is for SOAP, not for XML/HTTP
            throw new IllegalArgumentException();
        switch(version) {
        case SOAP_11:
            return new FastInfosetStreamSOAP11Encoder();
        case SOAP_12:
            return new FastInfosetStreamSOAP12Encoder();
        default:
            throw new AssertionError();
        }
    }
}
