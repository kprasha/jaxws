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
package com.sun.xml.ws.encoding.fastinfoset;

import com.sun.xml.fastinfoset.stax.StAXDocumentSerializer;
import com.sun.xml.fastinfoset.stax.StAXDocumentParser;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Messages;
import com.sun.xml.ws.api.pipe.Codec;
import com.sun.xml.ws.api.pipe.ContentType;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.encoding.ContentTypeImpl;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLStreamReader;
import javax.xml.ws.WebServiceException;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.nio.channels.ReadableByteChannel;
import org.jvnet.fastinfoset.FastInfosetSource;

/**
 * A codec for encoding/decoding XML infosets to/from fast
 * infoset documents.
 *
 * @author Paul Sandoz
 */
public class FastInfosetCodec implements Codec {
    private static final ContentType contentType = new ContentTypeImpl("application/fastinfoset");

    private StAXDocumentParser _parser;
    
    private StAXDocumentSerializer _serializer;
    
    /* package */ FastInfosetCodec() {
    }

    public String getMimeType() {
        return contentType.getContentType();
    }

    public Codec copy() { 
        return new FastInfosetCodec();
    }
    
    public ContentType getStaticContentType(Packet packet) {
        return contentType;
    }

    public ContentType encode(Packet packet, OutputStream out) {
        if (packet.getMessage() != null) {
            final XMLStreamWriter writer = getXMLStreamWriter(out);
            try {
                writer.writeStartDocument();
                packet.getMessage().writePayloadTo(writer);
                writer.writeEndDocument();
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

    public void decode(InputStream in, String contentType, Packet packet) throws IOException {
        Message message = Messages.createUsingPayload(new FastInfosetSource(in), 
                SOAPVersion.SOAP_11);
        packet.setMessage(message);
    }

    public void decode(ReadableByteChannel in, String contentType, Packet response) {
        throw new UnsupportedOperationException();
    }
    
    private XMLStreamWriter getXMLStreamWriter(OutputStream out) {
        if (_serializer != null) {
            _serializer.setOutputStream(out);
            return _serializer;
        } else {
            return _serializer = new StAXDocumentSerializer(out);
        }
    }

    private XMLStreamReader getXMLStreamReader(InputStream in) {
        if (_parser != null) {
            _parser.setInputStream(in);
            return _parser;
        } else {
            _parser = new StAXDocumentParser(in);
            _parser.setStringInterning(true);
            return _parser;
        }
    }

    /**
     * Creates a new {@link FastInfosetCodec} instance.
     */
    public static FastInfosetCodec create() {
        return new FastInfosetCodec();
    }
}