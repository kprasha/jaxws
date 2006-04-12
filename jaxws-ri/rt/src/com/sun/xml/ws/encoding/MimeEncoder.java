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

import com.sun.xml.messaging.saaj.packaging.mime.util.OutputUtil;
import com.sun.xml.ws.api.pipe.Encoder;
import com.sun.xml.ws.api.pipe.ContentType;
import com.sun.xml.ws.api.message.Attachment;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.streaming.XMLStreamWriterFactory;
import com.sun.xml.ws.encoding.ContentTypeImpl;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.ws.WebServiceException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.WritableByteChannel;

/**
 * {@link Encoder} that writes a {@link Message} and its attachments
 * as a MIME multipart message.
 *
 * @author Jitu
 */
public class MimeEncoder implements Encoder {
    /**
     * TODO: can we generate a content-id ? otherwise, buffering is required
     */
    public ContentType getStaticContentType(Packet packet) {
        return null;
    }

    // TODO: preencode String literals to byte[] so that they don't have to
    // go through char[]->byte[] conversion at runtime.

    public ContentType encode(Packet packet, OutputStream out) throws IOException {
        Message msg = packet.getMessage();

        String primaryCid = null;           // TODO
        String primaryCt = null;           // TODO

        com.sun.xml.messaging.saaj.packaging.mime.internet.ContentType contentType = new com.sun.xml.messaging.saaj.packaging.mime.internet.ContentType("multipart", "related", null);
        String boundary = UniqueValue.getUniqueBoundaryValue();
        contentType.setParameter("type", "text/xml");   // TODO
        contentType.setParameter("boundary", boundary);
        contentType.setParameter("start", primaryCid);

        String startBoundary = "--" + boundary;
        OutputUtil.writeln(startBoundary, out);     // write --boundary\r\n
        OutputUtil.writeln("Content-Id: " + primaryCid, out);
        OutputUtil.writeln("Content-Type: " + primaryCt, out);
        OutputUtil.writeln("Content-Transfer-Encoding: binary", out);
        OutputUtil.writeln(out);                    // write \r\n
        XMLStreamWriter writer = XMLStreamWriterFactory.createXMLStreamWriter(out);
        try {
            msg.writeTo(writer);
            writer.close();       // TODO Does this close stream ??
        } catch (XMLStreamException xe) {
            throw new WebServiceException(xe);
        }
        OutputUtil.writeln(out);                        // write \r\n

        // Encode all the attchments
        for (Attachment att : msg.getAttachments()) {
            OutputUtil.writeln(startBoundary, out);     // write --boundary\r\n
            OutputUtil.writeln("Content-Id: " + att.getContentId(), out);
            OutputUtil.writeln("Content-Type: " + att.getContentType(), out);
            OutputUtil.writeln("Content-Transfer-Encoding: binary", out);
            OutputUtil.writeln(out);                    // write \r\n
            att.writeTo(out);
            OutputUtil.writeln(out);                    // write \r\n
        }
        OutputUtil.writeAsAscii(startBoundary, out);    // write --boundary
        OutputUtil.writeAsAscii("--", out);

        return new ContentTypeImpl("multipart/related", null);
    }

    public ContentType encode(Packet packet, WritableByteChannel buffer) {
        //TODO: not yet implemented
        throw new UnsupportedOperationException();
    }

    public Encoder copy() {
        return this;
    }

    private static class UniqueValue {
        /**
         * A global part number.  Access is not synchronized because the
         * value is only one part of the unique value and so doesn't need
         * to be accurate.
         */
        private static int part = 0;

        /**
         * Get a unique value for use in a multipart boundary string.
         *
         * This implementation generates it by concatenating a global
         * part number, a newly created object's <code>hashCode()</code>,
         * and the current time (in milliseconds).
         */
        public static String getUniqueBoundaryValue() {
            StringBuffer s = new StringBuffer();

            // Unique string is ----=_Part_<part>_<hashcode>.<currentTime>
            s.append("----=_Part_").append(part++).append("_").
              append(s.hashCode()).append('.').
              append(System.currentTimeMillis());
            return s.toString();
        }
    }
}
