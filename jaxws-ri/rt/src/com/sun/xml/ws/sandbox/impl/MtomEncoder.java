package com.sun.xml.ws.sandbox.impl;

import com.sun.xml.stream.PropertyManager;
import com.sun.xml.stream.writers.XMLStreamWriterImpl;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.AttachmentSet;
import com.sun.xml.ws.api.message.Attachment;
import com.sun.xml.ws.api.pipe.Encoder;
import com.sun.xml.ws.streaming.XMLStreamWriterFactory;
import com.sun.xml.ws.sandbox.message.impl.stream.StreamAttachment;
import com.sun.xml.messaging.saaj.packaging.mime.util.OutputUtil;

import javax.activation.DataHandler;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.namespace.NamespaceContext;
import javax.xml.ws.WebServiceException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.io.UnsupportedEncodingException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.channels.WritableByteChannel;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.MalformedURLException;
import java.net.URLEncoder;

import org.jvnet.staxex.XMLStreamWriterEx;
import org.jvnet.staxex.NamespaceContextEx;

/**
 * Partial Implmentation of MTOM/XOP {@link Encoder}.
 * <p/>
 * This will typically be created by the
 * {@link com.sun.xml.ws.transport.http.client.HttpTransportPipe} or
 * {@link com.sun.xml.ws.transport.local.client.LocalTransportPipe} on the client side and
 * by {@link com.sun.xml.ws.api.pipe.Acceptor} on the server side.
 *
 * @author Vivek Pandey
 */
public class MtomEncoder implements Encoder {
    private static final String boundary = "uuid:" + UUID.randomUUID().toString();
    private static final String boundaryParameter = "boundary=\"" + boundary+"\"";
    private static final String xopContentType = "application/xop+xml";
    private final String soapXopContentType;
    private final XMLStreamWriterEx xmlStreamWriterEx = new MtomStreamWriter();
    private final String messageContentType;
    private final String soapContentType;
    private XMLStreamWriter writer;

    //This is the mtom attachment stream, we should write it just after the root part for decoder
    private final List<ByteArrayOutputStream> mtomAttachmentStream = new ArrayList<ByteArrayOutputStream>();

    public MtomEncoder(String contentType){
        this.soapContentType = contentType;
        this.messageContentType =  "Multipart/Related;type=\""+xopContentType+"\";" + boundaryParameter + ";start-info=\"" + soapContentType+"\"";
        this.soapXopContentType = xopContentType+";charset=utf-8;type=\""+soapContentType+"\"";
    }

    /**
     * Return the soap 1.1 and soap 1.2 specific XOP packaged ContentType
     *
     * @return A non-null content type for soap11 or soap 1.2 content type
     */
    public String getStaticContentType() {
        return messageContentType;
    }

    private OutputStream writeMtomBinary(String contentType){
        throw new UnsupportedOperationException();
    }

    public String encode(Packet packet, OutputStream out) throws IOException {
        this.writer = XMLStreamWriterFactory.createXMLStreamWriter(out);
        if(packet.getMessage() != null){
            try {
                //OutputUtil.writeln("Content-Type: "+messageContentType, out);
                //OutputUtil.writeln(out);                    // write \r\n
                OutputUtil.writeln("--"+boundary, out);
                OutputUtil.writeln("Content-Type: "+ soapXopContentType,  out);
                OutputUtil.writeln("Content-Transfer-Encoding: binary", out);
                OutputUtil.writeln(out);
                packet.getMessage().writeTo(getXmlStreamWriterEx());
                OutputUtil.writeln(out);
                //most of the writeTo calls writer.close(), does it close the outputStream also?

                int numOfAttachments = 0;
                Iterator<Attachment> mimeAttSet = packet.getMessage().getAttachments().iterator();
                for(ByteArrayOutputStream bos : mtomAttachmentStream){
                    out.write(bos.toByteArray());
                    if(++numOfAttachments != mtomAttachmentStream.size()){
                        OutputUtil.writeln(out);
                    }else{
                        OutputUtil.writeAsAscii("--", out);
                    }
                }

                //now write out the attachments in the message
                writeAttachments(packet.getMessage().getAttachments(),out);
            } catch (XMLStreamException e) {
                throw new WebServiceException(e);
            }
        }
        return messageContentType;
    }

    private static final String XOP_LOCALNAME = "Include";
    private static final String XOP_NAMESPACEURI = "http://www.w3.org/2004/08/xop/include";

    private void writeMtomBinary(byte[] data, int start, int len, String contentType){
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            String contentId = encodeCid(null);

            //build attachment frame
            OutputUtil.writeln("--"+boundary, bos);
            writeMimeHeaders(contentType, contentId, bos);
            bos.write(data, start, len);
            OutputUtil.writeln(bos);
            OutputUtil.writeAsAscii("--"+boundary, bos);
            mtomAttachmentStream.add(bos);

            //write out the xop reference
            try {
                String xopPrefix = writer.getPrefix(XOP_NAMESPACEURI);
                if(xopPrefix == null){
                    writer.setPrefix("xop", XOP_NAMESPACEURI);
                    writer.writeStartElement("xop", XOP_LOCALNAME, XOP_NAMESPACEURI);
                }else{
                    writer.writeStartElement(xopPrefix, XOP_LOCALNAME, XOP_NAMESPACEURI);
                }
                writer.writeAttribute("href", "cid:"+contentId);
            } catch (XMLStreamException e) {
                throw new WebServiceException(e);
            }
        } catch (IOException e) {
            throw new WebServiceException(e);
        }
    }

    private void writeMimeHeaders(String contentType, String contentId, OutputStream out) throws IOException {
        OutputUtil.writeln("Content-Type: " + contentType, out);
        OutputUtil.writeln("Content-Id: <" + contentId+">", out);
        OutputUtil.writeln("Content-Transfer-Encoding: binary", out);
        OutputUtil.writeln(out);
    }

    private void writeMtomBinary(DataHandler dataHandler){

    }

    private void writeAttachments(AttachmentSet attachments, OutputStream out) throws IOException {
        for(Attachment att : attachments){
            writeMimeHeaders(att.getContentType(), att.getContentId(), out);
            OutputUtil.writeln(out);                    // write \r\n
            att.writeTo(out);
            OutputUtil.writeln(out);                    // write \r\n
        }
    }

    public String encode(Packet packet, WritableByteChannel buffer) {
        throw new UnsupportedOperationException();
    }

    public Encoder copy() {
        throw new UnsupportedOperationException();
    }

    public XMLStreamWriterEx getXmlStreamWriterEx() {
        return xmlStreamWriterEx;
    }

    public XMLStreamWriterEx getXmlStreamWriterEx(OutputStream os) {
        this.writer = XMLStreamWriterFactory.createXMLStreamWriter(os);
        return xmlStreamWriterEx;
    }

    private String encodeCid(String ns){
        String cid="example.jaxws.sun.com";
        String name = UUID.randomUUID()+"@";
        if(ns != null && (ns.length() > 0)){
            try {
                URI uri = new URI(ns);
                String host = uri.toURL().getHost();
                cid = host;
            } catch (URISyntaxException e) {
                e.printStackTrace();
                return null;
            } catch (MalformedURLException e) {
                try {
                    cid = URLEncoder.encode(ns, "UTF-8");
                } catch (UnsupportedEncodingException e1) {
                    throw new WebServiceException(e);
                }
            }
        }
        return name + cid;
    }

    public static final Encoder SOAP11 = new MtomEncoder("text/xml");
    public static final Encoder SOAP12 = new MtomEncoder("application/soap+xml");

    public static Encoder get(SOAPVersion version) {
        if(version==null)
            // this decoder is for SOAP, not for XML/HTTP
            throw new IllegalArgumentException();
        switch(version) {
        case SOAP_11:
            return SOAP11;
        case SOAP_12:
            return SOAP12;
        default:
            throw new AssertionError();
        }
    }

    private class MtomStreamWriter implements XMLStreamWriterEx {
        public void writeBinary(byte[] data, int start, int len, String contentType) throws XMLStreamException {
            writeMtomBinary(data, start, len, contentType);
        }

        public void writeBinary(DataHandler dataHandler) throws XMLStreamException {
            writeMtomBinary(dataHandler);
        }

        public OutputStream writeBinary(String contentType) throws XMLStreamException {
            return writeMtomBinary(contentType);
        }

        public void writePCDATA(CharSequence data) throws XMLStreamException {
            throw new UnsupportedOperationException();
        }

        private class MtomNamespaceContextEx implements NamespaceContextEx {
            private NamespaceContext nsContext;

            public MtomNamespaceContextEx(NamespaceContext nsContext) {
                this.nsContext = nsContext;
            }

            public Iterator<Binding> iterator() {
                throw new UnsupportedOperationException();
            }

            public String getNamespaceURI(String prefix) {
                return nsContext.getNamespaceURI(prefix);
            }

            public String getPrefix(String namespaceURI) {
                return nsContext.getPrefix(namespaceURI);
            }

            public Iterator getPrefixes(String namespaceURI) {
                return nsContext.getPrefixes(namespaceURI);
            }

        }

        public void close() throws XMLStreamException {
            writer.close();
        }

        public void flush() throws XMLStreamException {
            writer.flush();
        }

        public void writeEndDocument() throws XMLStreamException {
            writer.writeEndDocument();
        }

        public void writeEndElement() throws XMLStreamException {
            writer.writeEndElement();
        }

        public void writeStartDocument() throws XMLStreamException {
            writer.writeStartDocument();
        }

        public void writeCharacters(char[] text, int start, int len) throws XMLStreamException {
            writer.writeCharacters(text, start, len);
        }

        public void setDefaultNamespace(String uri) throws XMLStreamException {
            writer.setDefaultNamespace(uri);
        }

        public void writeCData(String data) throws XMLStreamException {
            writer.writeCData(data);
        }

        public void writeCharacters(String text) throws XMLStreamException {
            writer.writeCharacters(text);
        }

        public void writeComment(String data) throws XMLStreamException {
            writer.writeComment(data);
        }

        public void writeDTD(String dtd) throws XMLStreamException {
            writer.writeDTD(dtd);
        }

        public void writeDefaultNamespace(String namespaceURI) throws XMLStreamException {
            writer.writeDefaultNamespace(namespaceURI);
        }

        public void writeEmptyElement(String localName) throws XMLStreamException {
            writer.writeEmptyElement(localName);
        }

        public void writeEntityRef(String name) throws XMLStreamException {
            writer.writeEntityRef(name);
        }

        public void writeProcessingInstruction(String target) throws XMLStreamException {
            writer.writeProcessingInstruction(target);
        }

        public void writeStartDocument(String version) throws XMLStreamException {
            writer.writeStartDocument(version);
        }

        public void writeStartElement(String localName) throws XMLStreamException {
            writer.writeStartElement(localName);
        }

        public NamespaceContextEx getNamespaceContext() {
            NamespaceContext nsContext = writer.getNamespaceContext();
            return new MtomNamespaceContextEx(nsContext);
        }

        public void setNamespaceContext(NamespaceContext context) throws XMLStreamException {
            writer.setNamespaceContext(context);
        }

        public Object getProperty(String name) throws IllegalArgumentException {
            return writer.getProperty(name);
        }

        public String getPrefix(String uri) throws XMLStreamException {
            return writer.getPrefix(uri);
        }

        public void setPrefix(String prefix, String uri) throws XMLStreamException {
            writer.setPrefix(prefix, uri);
        }

        public void writeAttribute(String localName, String value) throws XMLStreamException {
            writer.writeAttribute(localName, value);
        }

        public void writeEmptyElement(String namespaceURI, String localName) throws XMLStreamException {
            writer.writeEmptyElement(namespaceURI, localName);
        }

        public void writeNamespace(String prefix, String namespaceURI) throws XMLStreamException {
            writer.writeNamespace(prefix, namespaceURI);
        }

        public void writeProcessingInstruction(String target, String data) throws XMLStreamException {
            writer.writeProcessingInstruction(target, data);
        }

        public void writeStartDocument(String encoding, String version) throws XMLStreamException {
            writer.writeStartDocument(encoding, version);
        }

        public void writeStartElement(String namespaceURI, String localName) throws XMLStreamException {
            writer.writeStartElement(namespaceURI, localName);
        }

        public void writeAttribute(String namespaceURI, String localName, String value) throws XMLStreamException {
            writer.writeAttribute(namespaceURI, localName, value);
        }

        public void writeEmptyElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
            writer.writeEmptyElement(prefix, localName, namespaceURI);
        }

        public void writeStartElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
            writer.writeStartElement(prefix, localName, namespaceURI);
        }

        public void writeAttribute(String prefix, String namespaceURI, String localName, String value) throws XMLStreamException {
            writer.writeAttribute(prefix, namespaceURI, localName, value);
        }
    }
}
