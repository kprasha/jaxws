package com.sun.xml.ws.sandbox.impl;

import com.sun.xml.ws.api.message.AttachmentSet;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.pipe.Encoder;
import com.sun.xml.ws.api.pipe.ContentType;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.encoding.xml.XMLMessage;
import com.sun.xml.ws.streaming.XMLStreamWriterFactory;
import java.io.IOException;
import java.io.InputStream;
import javax.activation.DataSource;
import com.sun.xml.ws.encoding.xml.XMLMessage.HasDataSource;
import com.sun.xml.messaging.saaj.util.ByteOutputStream;

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
        Message msg = packet.getMessage();
        if (msg == null) {
            return null;
        }
        if (msg instanceof HasDataSource ) {
            HasDataSource hasDS = (HasDataSource)msg;
            if (hasDS.hasUnconsumedDataSource()) {
                return new ContentTypeImpl(hasDS.getDataSource().getContentType());
            }
            return null;
        }
        return contentType;
    }

    public ContentType encode(Packet packet, OutputStream out) {
        Message msg = packet.getMessage();
        if (msg == null) {
            return null;
        }
        if (msg instanceof HasDataSource ) {
            HasDataSource hasDS = (HasDataSource)msg;
            if (hasDS.hasUnconsumedDataSource()) {
                try {
                    DataSource ds = hasDS.getDataSource();
                    InputStream is = ds.getInputStream();
                    byte[] buf = new byte[1024];
                    int count;
                    while((count=is.read(buf)) != -1) {
                        out.write(buf, 0, count);
                    }
                    return new ContentTypeImpl(ds.getContentType());
                } catch(IOException ioe) {
                    throw new WebServiceException(ioe);
                }
            } else {
                // TODO
                throw new UnsupportedOperationException("TODO: encode");
            }
        } else {
            XMLStreamWriter writer = XMLStreamWriterFactory.createXMLStreamWriter(out);
            try {
                packet.getMessage().writePayloadTo(writer);
                writer.flush();
            } catch (XMLStreamException e) {
                throw new WebServiceException(e);
            }
            return contentType;
        }
    }
    
    public static DataSource createDataSource(Message msg) {
        if (msg instanceof HasDataSource) {
            return ((HasDataSource)msg).getDataSource();
        } else {
            AttachmentSet atts = msg.getAttachments();
            if (atts != null && atts != atts.EMPTY) {
                final ByteOutputStream bos = new ByteOutputStream();
                try {
                    ContentType ct = new MimeEncoder().encode(new Packet(msg), bos);
                    return XMLMessage.createDataSource(ct.getContentType(), bos.newInputStream());
                } catch(IOException ioe) {
                    throw new WebServiceException(ioe);
                }
                
            } else {
                final ByteOutputStream bos = new ByteOutputStream();
                XMLStreamWriter writer = XMLStreamWriterFactory.createXMLStreamWriter(bos);
                try {
                    msg.writePayloadTo(writer);
                    writer.flush();
                } catch (XMLStreamException e) {
                    throw new WebServiceException(e);
                }
                return XMLMessage.createDataSource("text/xml", bos.newInputStream());
            }

        }
        
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
