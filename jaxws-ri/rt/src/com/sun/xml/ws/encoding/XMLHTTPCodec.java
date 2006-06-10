/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License).  You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the license at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * 
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.xml.ws.encoding;

import com.sun.xml.messaging.saaj.util.ByteOutputStream;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.message.AttachmentSet;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.Codec;
import com.sun.xml.ws.api.pipe.ContentType;
import com.sun.xml.ws.encoding.xml.XMLMessage;
import com.sun.xml.ws.encoding.xml.XMLMessage.HasDataSource;
import com.sun.xml.ws.streaming.XMLStreamWriterFactory;

import javax.activation.DataSource;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.ws.WebServiceException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * {@link Codec} that just writes the XML in XML/HTTP binding
 *
 * @author Jitendra Kotamraju
 */
public final class XMLHTTPCodec implements Codec {

    private final ContentType contentType;

    private XMLHTTPCodec(String contentType) {
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
                    Codec codec = new SwACodec(SOAPVersion.SOAP_11);
                    ContentType ct = codec.getStaticContentType(new Packet(msg));
                    codec.encode(new Packet(msg), bos);
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

    public void decode(InputStream in, String contentType, Packet packet) throws IOException {
        Message message = XMLMessage.create(contentType, in);
        packet.setMessage(message);
    }

    public Message decodeXMLMultipart(InputStream in, String contentType) throws IOException {
        XMLHTTPMultipartDecoder decoder = new XMLHTTPMultipartDecoder();
        return decoder.decode(in, contentType);
    }

    public void decode(ReadableByteChannel in, String contentType, Packet packet) {
        // TODO
        throw new UnsupportedOperationException();
    }

    public static class XMLHTTPMultipartDecoder {
        public Message decode(InputStream in, String contentType) throws IOException {
            MimeMultipartParser parser = new MimeMultipartParser(in, contentType);
           
            //decoder.decode(in, contentType);
            // TODO need to create a Message around MimeMultipartRelatedDecoder
            return new XMLMessage.XMLMultiPartParsedMessage(contentType, parser);
            //return XMLMessage.create(decoder);
        }

    }

    public Codec copy() {
        return this;
    }

    public static final XMLHTTPCodec INSTANCE = new XMLHTTPCodec("text/xml");

    static class ContentTypeImpl implements ContentType {
        final String contentType;

        ContentTypeImpl(String contentType) {
            this.contentType = contentType;
        }
        public String getContentType() {
            return contentType;
        }
        public String getSOAPActionHeader() {
            return null;
        }
        public String getAcceptHeader() {
            return null;
        }
    }
}
