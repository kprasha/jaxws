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
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.message.Attachment;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.Codec;
import com.sun.xml.ws.api.pipe.ContentType;
import com.sun.xml.ws.message.stream.StreamAttachment;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.WritableByteChannel;
import java.util.UUID;

/**
 * {@link Codec} that uses MIME/multipart as the base format.
 *
 * @author Jitendra Kotamraju
 */
public final class SwACodec extends MimeCodec {

    private String boundary;
    private boolean hasAttachments;

    private final StreamSOAPCodec soapCodec;

    public SwACodec(SOAPVersion version) {
        super(version);
        this.soapCodec = StreamSOAPCodec.create(version);
    }

    private SwACodec(SwACodec that) {
        super(that);
        this.soapCodec = that.soapCodec.copy();
    }

    public ContentType getStaticContentType(Packet packet) {
        Message msg = packet.getMessage();
        hasAttachments = !msg.getAttachments().isEmpty();

        if (hasAttachments) {
            boundary = "uuid:" + UUID.randomUUID().toString();
            String boundaryParameter = "boundary=\"" + boundary +"\"";
            // TODO use primaryEncoder to get type
            String messageContentType =  "Multipart/Related; type=\"text/xml\"; "+boundaryParameter;
            return new ContentTypeImpl(messageContentType, packet.soapAction, null);
        } else {
            return soapCodec.getStaticContentType(packet);
        }
    }

    @Override
    protected void decode(MimeMultipartParser mpp, Packet packet) throws IOException {
        // TODO: handle attachments correctly
        StreamAttachment root = mpp.getRootPart();
        soapCodec.decode(root.asInputStream(),root.getContentType(),packet);
    }


    // TODO: preencode String literals to byte[] so that they don't have to
    // go through char[]->byte[] conversion at runtime.

    public ContentType encode(Packet packet, OutputStream out) throws IOException {
        Message msg = packet.getMessage();
        if (msg == null) {
            return null;
        }

        if (hasAttachments) {
            OutputUtil.writeln("--"+boundary, out);
            OutputUtil.writeln("Content-Type: text/xml", out);
            OutputUtil.writeln(out);
        }
        ContentType primaryCt = soapCodec.encode(packet, out);

        if (hasAttachments) {
            OutputUtil.writeln(out);
            // Encode all the attchments
            for (Attachment att : msg.getAttachments()) {
                OutputUtil.writeln("--"+boundary, out);
                OutputUtil.writeln("Content-Id: <" + att.getContentId()+">", out);
                OutputUtil.writeln("Content-Type: " + att.getContentType(), out);
                OutputUtil.writeln("Content-Transfer-Encoding: binary", out);
                OutputUtil.writeln(out);                    // write \r\n
                att.writeTo(out);
                OutputUtil.writeln(out);                    // write \r\n
            }
            OutputUtil.writeAsAscii("--"+boundary, out);
            OutputUtil.writeAsAscii("--", out);
        }
        return hasAttachments ? new ContentTypeImpl("multipart/related", null, null) : primaryCt;
    }

    public ContentType encode(Packet packet, WritableByteChannel buffer) {
        //TODO: not yet implemented
        throw new UnsupportedOperationException();
    }

    public SwACodec copy() {
        return new SwACodec(this);
    }
}
