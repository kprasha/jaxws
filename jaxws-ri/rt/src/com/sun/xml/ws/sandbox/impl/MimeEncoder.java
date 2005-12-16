package com.sun.xml.ws.sandbox.impl;

import com.sun.xml.messaging.saaj.packaging.mime.internet.ContentType;
import com.sun.xml.messaging.saaj.packaging.mime.util.OutputUtil;
import com.sun.xml.ws.sandbox.Encoder;
import com.sun.xml.ws.sandbox.XMLStreamWriterEx;
import com.sun.xml.ws.sandbox.message.Attachment;
import com.sun.xml.ws.sandbox.message.Message;
import com.sun.xml.ws.streaming.XMLStreamWriterFactory;

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
    public String getStaticContentType() {
        return null;
    }

    // TODO: preencode String literals to byte[] so that they don't have to
    // go through char[]->byte[] conversion at runtime.

    public String encode(Message message, OutputStream out) throws IOException {

        String primaryCid = null;           // TODO
        String primaryCt = null;           // TODO

        ContentType contentType = new ContentType("multipart", "related", null);
        String boundary = UniqueValue.getUniqueBoundaryValue();
        contentType.setParameter("type", "text/xml");   // TODO
        contentType.setParameter("boundary", boundary);
        contentType.setParameter("start", primaryCid);

        String startBoundary = "--" + boundary;
        OutputUtil.writeln(startBoundary, out);     // write --boundary\r\n
        OutputUtil.writeln("Content-Id: " + primaryCid, out);
        OutputUtil.writeln("Content-Type: " + primaryCt, out);
        OutputUtil.writeln(out);                    // write \r\n
        XMLStreamWriter writer = XMLStreamWriterFactory.createXMLStreamWriter(out);
        try {
            message.writeTo(writer);
            writer.close();       // TODO Does this close stream ??
        } catch (XMLStreamException xe) {
            throw new WebServiceException(xe);
        }
        OutputUtil.writeln(out);                        // write \r\n

        // Encode all the attchments
        for (Attachment att : message.getAttachments()) {
            OutputUtil.writeln(startBoundary, out);     // write --boundary\r\n
            OutputUtil.writeln("Content-Id: " + att.getContentId(), out);
            OutputUtil.writeln("Content-Type: " + att.getContentType(), out);
            OutputUtil.writeln(out);                    // write \r\n
            att.writeTo(out);
            OutputUtil.writeln(out);                    // write \r\n
        }
        OutputUtil.writeAsAscii(startBoundary, out);    // write --boundary
        OutputUtil.writeAsAscii("--", out);

        return "multipart/related";
    }

    public String encode(Message message, WritableByteChannel buffer) {
        //TODO: not yet implemented
        throw new UnsupportedOperationException();
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
