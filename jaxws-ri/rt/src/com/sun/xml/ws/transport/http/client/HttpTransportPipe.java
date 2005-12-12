/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 * 
 * You can obtain a copy of the license at
 * https://jwsdp.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL
 * HEADER in each file and include the License file at
 * https://jwsdp.dev.java.net/CDDLv1.0.html  If applicable,
 * add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your
 * own identifying information: Portions Copyright [yyyy]
 * [name of copyright owner]
 */
package com.sun.xml.ws.transport.http.client;

import com.sun.xml.messaging.saaj.packaging.mime.internet.ContentType;
import com.sun.xml.messaging.saaj.packaging.mime.util.OutputUtil;
import com.sun.xml.ws.handler.MessageContextImpl;
import com.sun.xml.ws.sandbox.Decoder;
import com.sun.xml.ws.sandbox.Encoder;
import com.sun.xml.ws.sandbox.XMLStreamWriterEx;
import com.sun.xml.ws.sandbox.message.Attachment;
import com.sun.xml.ws.sandbox.message.AttachmentSet;
import com.sun.xml.ws.sandbox.message.Message;
import com.sun.xml.ws.sandbox.message.MessageProperties;
import com.sun.xml.ws.sandbox.pipe.Pipe;
import com.sun.xml.ws.spi.runtime.WSConnection;
import com.sun.xml.ws.spi.runtime.WebServiceContext;
import com.sun.xml.ws.streaming.XMLStreamWriterFactory;
import com.sun.xml.ws.transport.local.server.LocalConnectionImpl;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.ws.WebServiceException;

/**
 *
 * @author jitu
 */
public class HttpTransportPipe implements Pipe {
    
    public HttpTransportPipe() {
    }

    public void postConstruct() {
    }

    public Message process(Message msg) {
        
        try {
            // Set up WSConnection with tranport headers, request content
            // TODO: remove WSConnection based HttpClienTransport
            WSConnection con = new HttpClientTransport();
            
            // get transport headers from message
            MessageProperties props = msg.getProperties();
            Map<String, List<String>> reqHeaders = props.getHttpRequestHeaders();
            con.setHeaders(reqHeaders);

            Encoder encoder = new EnvelopeEncoder();
            encoder.encode(msg, con.getOutput());
            
            Decoder decoder = new EnvelopeDecoder();
            Map<String, List<String>> respHeaders = con.getHeaders();
            String ct = getContentType(respHeaders);
            return decoder.decode(con.getInput(), ct);
        } catch(WebServiceException wex) {
            throw wex;
        } catch(Exception ex) {
            throw new WebServiceException(ex);
        }
    }
    
    private String getContentType(Map<String, List<String>> headers) {
        return null;
    }

    public void preDestroy() {
    }
    
    /**
     * Correct Content-Type transport header is known only after encode().
     * transport needs to buffer the encoded contents, and then it needs to be
     * written to transport connection.
     */
    private static class MimeEncoder implements Encoder {
        
        /*
         * can we generate a content-id ? otherwise, buffering is required
         */
        public String getStaticContentType() {
            return null;
        }

        public String encode(Message message, OutputStream out) throws IOException {
            
            String primaryCid = null;           // TODO
            String primaryCt = null;           // TODO
            String partCid = null;
            String partCt = null;
            
            ContentType contentType = new ContentType();
            String boundary = UniqueValue.getUniqueBoundaryValue();
            contentType = new ContentType("multipart", "related", null);
            contentType.setParameter("type", "text/xml");   // TODO
            contentType.setParameter("boundary", boundary);
            contentType.setParameter("start", primaryCid);
            
            String startBoundary = "--" + boundary;
            OutputUtil.writeln(startBoundary, out);     // write --boundary\r\n
            partCid = "Content-Id: "+primaryCid;
            OutputUtil.writeln(partCid, out);
            partCt = "Content-Type: "+primaryCt;
            OutputUtil.writeln(primaryCt, out);
            OutputUtil.writeln(out);                    // write \r\n 
            XMLStreamWriterEx writer = new XMLStreamWriterExImpl(out);
            message.writeTo(writer);
            try {
                writer.getBase().close();       // TODO Does this close stream ??
            } catch(XMLStreamException xe) {
                throw new WebServiceException(xe);
            }
            OutputUtil.writeln(out);                        // write \r\n   
            
            // Encode all the attchments
            AttachmentSet attSet = message.getAttachments();
            Iterator<Attachment> it = attSet.iterator();
            while(it.hasNext()) {
                Attachment att = it.next();
                OutputUtil.writeln(startBoundary, out);     // write --boundary\r\n
                partCid = "Content-Id: "+att.getContentId();
                OutputUtil.writeln(partCid, out);
                partCt = "Content-Type: "+att.getContentType();
                OutputUtil.writeln(partCt, out);
                OutputUtil.writeln(out);                    // write \r\n            
                att.writeTo(out);
                OutputUtil.writeln(out);                    // write \r\n    
            }
            OutputUtil.writeAsAscii(startBoundary, out);    // write --boundary
            OutputUtil.writeAsAscii("--", out);
                
            return contentType.toString();
        }

        public String encode(Message message, ByteBuffer buffer) {
            //TODO: not yet implemented
            throw new UnsupportedOperationException();
        }
        
    }
    
    
    private static class EnvelopeEncoder implements Encoder {
        public String getStaticContentType() {
            return "text/xml";
        }

        public String encode(Message message, OutputStream out) throws IOException {
            // TODO attachments, XOP
            XMLStreamWriterEx writer = new XMLStreamWriterExImpl(out);
            message.writeTo(writer);
            return "text/xml";
        }

        public String encode(Message message, ByteBuffer buffer) {
            //TODO: not yet implemented
            throw new UnsupportedOperationException();
        }
        
    }
        
    private static class EnvelopeDecoder implements Decoder {
        
        public Message decode(InputStream in, String contentType) throws IOException {
            Message msg = null;
            // msg = new StreamMessage(in);
            //TODO: not yet implemented
            throw new UnsupportedOperationException();
        }

        public Message decode(ByteBuffer in, String contentType) {
            //TODO: not yet implemented
            throw new UnsupportedOperationException();
        }
        
    }
    
    private static class XMLStreamWriterExImpl implements XMLStreamWriterEx {
        private OutputStream out;
        private XMLStreamWriter writer;
        
        public XMLStreamWriterExImpl(OutputStream out) {
            this.out = out;
            writer = XMLStreamWriterFactory.createXMLStreamWriter(out);
        }

        public XMLStreamWriter getBase() {
            return writer;
        }

        public void writeBinary(byte[] data, int start, int len) throws XMLStreamException {
            //TODO: not yet implemented
            throw new UnsupportedOperationException();
        }
        
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
