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
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.xml.ws.encoding.xml;

import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.Codec;
import com.sun.xml.ws.api.pipe.ContentType;
import com.sun.xml.ws.api.streaming.XMLStreamWriterFactory;
import com.sun.xml.ws.encoding.ContentTypeImpl;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.ws.WebServiceException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

public final class XMLCodec implements Codec {
    public static final String XML_APPLICATION_MIME_TYPE = "application/xml";

    public static final String XML_TEXT_MIME_TYPE = "text/xml";

    private static final ContentType contentType = new ContentTypeImpl(XML_TEXT_MIME_TYPE);

    public String getMimeType() {
        return XML_APPLICATION_MIME_TYPE;
    }

    public ContentType getStaticContentType(Packet packet) {
        return contentType;
    }

    public ContentType encode(Packet packet, OutputStream out) {
        XMLStreamWriter writer = XMLStreamWriterFactory.create(out);
        try {
            if (packet.getMessage().hasPayload()){
                packet.getMessage().writePayloadTo(writer);
                writer.flush();
            }
        } catch (XMLStreamException e) {
            throw new WebServiceException(e);
        }
        return contentType;
    }

    public ContentType encode(Packet packet, WritableByteChannel buffer) {
        //TODO: not yet implemented
        throw new UnsupportedOperationException();
    }

    public Codec copy() {
        return this;
    }

    public void decode(InputStream in, String contentType, Packet packet) throws IOException {
        Message message = XMLMessage.create(contentType, in);
        packet.setMessage(message);
    }

    public void decode(ReadableByteChannel in, String contentType, Packet packet) {
        // TODO
        throw new UnsupportedOperationException();
    }    
}
