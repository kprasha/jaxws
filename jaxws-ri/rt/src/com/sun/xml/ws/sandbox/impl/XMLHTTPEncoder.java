package com.sun.xml.ws.sandbox.impl;

import com.sun.xml.ws.api.pipe.Encoder;
import com.sun.xml.ws.api.pipe.ContentType;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.streaming.XMLStreamWriterFactory;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.ws.WebServiceException;
import java.io.OutputStream;
import java.nio.channels.WritableByteChannel;

/**
 * {@link Encoder} that just writes the XML in XML/HTTP binding
 *
 * @author Jitendra Kotamraju
 */
public final class XMLHTTPEncoder implements Encoder {

    private final ContentType contentType;

    private XMLHTTPEncoder(String contentType) {
        this.contentType = new ContentTypeImpl(contentType);
    }

    public ContentType getStaticContentType(Packet packet) {
        return contentType;
    }

    public ContentType encode(Packet packet, OutputStream out) {
        if (packet.getMessage() != null) {
            XMLStreamWriter writer = XMLStreamWriterFactory.createXMLStreamWriter(out);
            try {
                packet.getMessage().writePayloadTo(writer);
                writer.flush();
            } catch (XMLStreamException e) {
                throw new WebServiceException(e);
            }
        }
        return contentType;
    }

    public ContentType encode(Packet packet, WritableByteChannel buffer) {
        //TODO: not yet implemented
        throw new UnsupportedOperationException();
    }

    public Encoder copy() {
        return this;
    }
    
    public static final Encoder INSTANCE = new XMLHTTPEncoder("text/xml");
    
    static class ContentTypeImpl implements ContentType {
        final String contentType;
        
        ContentTypeImpl(String contentType) {
            this.contentType = contentType;
        }
        public String getContentType() {
            return contentType;
        }
        public String getSOAPAction() {
            return null;
        }
    }
}
