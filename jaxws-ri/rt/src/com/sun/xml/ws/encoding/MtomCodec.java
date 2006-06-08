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
import com.sun.xml.stream.writers.UTF8OutputStreamWriter;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.message.Attachment;
import com.sun.xml.ws.api.message.AttachmentSet;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.Codec;
import com.sun.xml.ws.api.pipe.ContentType;
import com.sun.xml.ws.message.stream.StreamAttachment;
import com.sun.xml.ws.streaming.XMLStreamReaderFactory;
import com.sun.xml.ws.streaming.XMLStreamWriterFactory;
import org.jvnet.staxex.Base64Data;
import org.jvnet.staxex.NamespaceContextEx;
import org.jvnet.staxex.XMLStreamReaderEx;
import org.jvnet.staxex.XMLStreamWriterEx;

import javax.activation.DataHandler;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.ws.WebServiceException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Mtom messge Codec. It can be used even for non-soap message's mtom encoding.
 *
 * @author Vivek Pandey
 */
public class MtomCodec extends MimeCodec {
    // encoding
    private String boundaryParameter;
    private String boundary;
    private final String soapXopContentType;
    private final XMLStreamWriterEx xmlStreamWriterEx = new MtomStreamWriter();
    private String messageContentType;
    private XMLStreamWriter writer;
    private UTF8OutputStreamWriter osWriter;

    // decoding
    private final StreamSOAPCodec codec;
    private MimeMultipartParser mimeMP;
    private final MtomXMLStreamReaderEx xmlStreamReaderEx = new MtomXMLStreamReaderEx();
    private XMLStreamReader reader;
    private Base64Data base64AttData;
    private boolean xopReferencePresent = false;

    //values that will set to whether mtom or not as caller can call getPcData or getTextCharacters
    private int textLength;
    private int textStart;

    //To be used with #getTextCharacters
    private char[] base64EncodedText;

    //public MtomDecoder(MimeMultipartRelatedDecoder mimeMultipartDecoder) {
    //    this.mimeMultipartDecoder = mimeMultipartDecoder;
    //}



    //This is the mtom attachment stream, we should write it just after the root part for decoder
    private final List<ByteArrayBuffer> mtomAttachmentStream = new ArrayList<ByteArrayBuffer>();

    MtomCodec(SOAPVersion version){
        super(version);
        this.codec = StreamSOAPCodec.create(version);
        createConteTypeHeader();
        this.soapXopContentType = XOP_CONTENT_TYPE +";charset=utf-8;type=\""+version.contentType+"\"";
    }

    private void createConteTypeHeader(){
        boundary = "uuid:" + UUID.randomUUID().toString();
        boundaryParameter = "boundary=\"" + boundary +"\"";
        messageContentType =  "Multipart/Related;type=\""+XOP_CONTENT_TYPE +"\";" + boundaryParameter + ";start-info=\"" + version.contentType+"\"";
    }

    /**
     * Return the soap 1.1 and soap 1.2 specific XOP packaged ContentType
     *
     * @return A non-null content type for soap11 or soap 1.2 content type
     */
    public ContentType getStaticContentType(Packet packet) {
        return getContentType(packet);
    }

    private ContentType getContentType(Packet packet){
        switch(version){
            case SOAP_11:
                return new ContentTypeImpl(messageContentType, (packet.soapAction == null)?"":packet.soapAction, null);
            case SOAP_12:
                if(packet.soapAction != null){
                    messageContentType += ";action=\""+packet.soapAction+"\"";
                }
                return new ContentTypeImpl(messageContentType, null, null);
        }
        //never happens
        return null;
    }

    private OutputStream writeMtomBinary(String contentType){
        throw new UnsupportedOperationException();
    }

    public ContentType encode(Packet packet, OutputStream out) throws IOException {
        //get the current boundary thaat will be reaturned from this method
        mtomAttachmentStream.clear();
        ContentType contentType = getContentType(packet);
        writer = XMLStreamWriterFactory.createXMLStreamWriter(out);
        osWriter = new UTF8OutputStreamWriter(out);
        if(packet.getMessage() != null){
            try {
                OutputUtil.writeln("--"+boundary, out);
                OutputUtil.writeln("Content-Type: "+ soapXopContentType,  out);
                OutputUtil.writeln("Content-Transfer-Encoding: binary", out);
                OutputUtil.writeln(out);
                packet.getMessage().writeTo(getXmlStreamWriterEx());
                OutputUtil.writeln(out);

                int numOfAttachments = 0;
                Iterator<Attachment> mimeAttSet = packet.getMessage().getAttachments().iterator();

                //if there no other mime parts to be written, write the end boundary
                if(!mimeAttSet.hasNext() && mtomAttachmentStream.size() == 0){
                    OutputUtil.writeAsAscii("--"+boundary, out);
                    OutputUtil.writeAsAscii("--", out);
                }

                for(ByteArrayBuffer bos : mtomAttachmentStream){
                    bos.write(out);

                    //once last attachment is written, end with boundary
                    if(++numOfAttachments == mtomAttachmentStream.size()){
                        OutputUtil.writeAsAscii("--"+boundary, out);
                        OutputUtil.writeAsAscii("--", out);
                    }
                }

                //now write out the attachments in the message
                writeAttachments(packet.getMessage().getAttachments(),out);
            } catch (XMLStreamException e) {
                throw new WebServiceException(e);
            }
        }
        //now create the boundary for next encode() call
        createConteTypeHeader();
        return contentType;
    }

    private class ByteArrayBuffer{
        private  byte[] buff;
        private  int off;
        private  int len;
        private  String contentType;
        String contentId;

        ByteArrayBuffer(byte[] buff, int off, int len, String contentType) {
            this.buff = buff;
            this.off = off;
            this.len = len;
            this.contentType = contentType;
            this.contentId = encodeCid(null);
        }

        void write(OutputStream os) throws IOException {
            //build attachment frame
            OutputUtil.writeln("--"+boundary, os);
            writeMimeHeaders(contentType, contentId, os);
            os.write(buff, off, len);
            OutputUtil.writeln(os);
        }
    }

    private void writeMtomBinary(byte[] data, int start, int len, String contentType){
        try {
            ByteArrayBuffer bos = new ByteArrayBuffer(data, start, len, contentType);
            mtomAttachmentStream.add(bos);

            //write out the xop reference
            writer.writeCharacters("");
            osWriter.write(XOP_PREF +bos.contentId+XOP_SUFF);
        } catch (IOException e) {
            throw new WebServiceException(e);
        } catch (XMLStreamException e) {
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
        throw new UnsupportedOperationException();
    }

    private void writeAttachments(AttachmentSet attachments, OutputStream out) throws IOException {
        for(Attachment att : attachments){
            writeMimeHeaders(att.getContentType(), att.getContentId(), out);
            OutputUtil.writeln(out);                    // write \r\n
            att.writeTo(out);
            OutputUtil.writeln(out);                    // write \r\n
        }
    }

    public ContentType encode(Packet packet, WritableByteChannel buffer) {
        throw new UnsupportedOperationException();
    }

    public MtomCodec copy() {
        return new MtomCodec(version);
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
                cid = uri.toURL().getHost();
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

    @Override
    protected void decode(MimeMultipartParser mpp, Packet packet) throws IOException {
        this.mimeMP = mpp;
        reader = XMLStreamReaderFactory.createXMLStreamReader(mimeMP.getRootPart().asInputStream(), true);
        packet.setMessage(codec.decode(xmlStreamReaderEx));
    }

    private CharSequence getMtomPCData() {
        if(xopReferencePresent){
            return base64AttData;
        }
        return reader.getText();
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
            if(data == null)
                return;
            if(data instanceof Base64Data){
                Base64Data binaryData = (Base64Data)data;
                writeBinary(binaryData.getExact(), 0, binaryData.getDataLen(), binaryData.getMimeType());
                return;
            }
            writeCharacters(data.toString());
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

    private class MtomXMLStreamReaderEx implements XMLStreamReaderEx {
        public CharSequence getPCDATA() throws XMLStreamException {
            return getMtomPCData();
        }

        public NamespaceContextEx getNamespaceContext() {
            NamespaceContext nsContext = reader.getNamespaceContext();
            return new MtomNamespaceContextEx(nsContext);


        }

        public String getElementTextTrim() throws XMLStreamException {
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

        public int getAttributeCount() {
            return reader.getAttributeCount();
        }

        public int getEventType() {
            return reader.getEventType();
        }

        public int getNamespaceCount() {
            return reader.getNamespaceCount();
        }

        public int getTextLength() {
            if (xopReferencePresent)
                return textLength;
            return reader.getTextLength();
        }

        public int getTextStart() {
            //TODO: check if this is correct
            if (xopReferencePresent)
                return 0;
            return reader.getTextStart();
        }

        private class Base64DataEx extends Base64Data{
            private final InputStream is;
            public Base64DataEx(InputStream is) {
                super();
                this.is = is;
            }
        }
        public int next() throws XMLStreamException {
            int event = reader.next();
            if ((event == XMLStreamConstants.START_ELEMENT) && reader.getLocalName().equals(XOP_LOCALNAME) && reader.getNamespaceURI().equals(XOP_NAMESPACEURI))
            {
                //its xop reference, take the URI reference
                String href = reader.getAttributeValue(null, "href");
                href = decodeCid(href);
                try {
                    StreamAttachment att = mimeMP.getAttachmentPart(href);
                    base64AttData = att.asBase64Data();
                    textLength = base64AttData.getDataLen();
                    textStart = 0;
                    xopReferencePresent = true;
                } catch (IOException e) {
                    throw new WebServiceException(e);
                }
                //move to the </xop:Include>
                try {
                    reader.next();
                } catch (XMLStreamException e) {
                    throw new WebServiceException(e);
                }
                return XMLStreamConstants.CHARACTERS;
            }
            if(xopReferencePresent){
                xopReferencePresent = false;
                textStart = 0;
                textLength = 0;
                base64EncodedText = null;
            }
            return event;
        }

        private String decodeCid(String cid) {
            if (cid.startsWith("cid:"))
                cid = cid.substring(4, cid.length());
            try {
                return "<" + URLDecoder.decode(cid, "UTF-8") + ">";
            } catch (UnsupportedEncodingException e) {
                throw new WebServiceException(e);
            }
        }

        public int nextTag() throws XMLStreamException {
            return reader.nextTag();
        }

        public void close() throws XMLStreamException {
            reader.close();
        }

        public boolean hasName() {
            return reader.hasName();
        }

        public boolean hasNext() throws XMLStreamException {
            return reader.hasNext();
        }

        public boolean hasText() {
            return reader.hasText();
        }

        public boolean isCharacters() {
            return reader.isCharacters();
        }

        public boolean isEndElement() {
            return reader.isEndElement();
        }

        public boolean isStandalone() {
            return reader.isStandalone();
        }

        public boolean isStartElement() {
            return reader.isStartElement();
        }

        public boolean isWhiteSpace() {
            return reader.isWhiteSpace();
        }

        public boolean standaloneSet() {
            return reader.standaloneSet();
        }

        public char[] getTextCharacters() {
            if (xopReferencePresent) {
                char[] chars = new char[base64AttData.length()];
                base64AttData.writeTo(chars, 0);
                textLength = chars.length;
                return chars;
            }
            return reader.getTextCharacters();
        }

        public boolean isAttributeSpecified(int index) {
            return reader.isAttributeSpecified(index);
        }

        public int getTextCharacters(int sourceStart, char[] target, int targetStart, int length) throws XMLStreamException {
            if(xopReferencePresent){
                int event = reader.getEventType();
                if(event != XMLStreamConstants.CHARACTERS){
                    //its invalid state - delegate it to underlying reader to throw the corrrect exception so that user
                    // always sees the uniform exception from the XMLStreamReader
                    throw new XMLStreamException("Invalid state: Expected CHARACTERS found :");
                }
                if(target == null){
                    throw new NullPointerException("target char array can't be null") ;
                }

                if(targetStart < 0 || length < 0 || sourceStart < 0 || targetStart >= target.length ||
                        (targetStart + length ) > target.length) {
                    throw new IndexOutOfBoundsException();
                }

                if(base64EncodedText != null){
                    base64EncodedText = new char[base64AttData.length()];
                    base64AttData.writeTo(base64EncodedText, 0);
                    textLength = base64EncodedText.length;
                    textStart = 0;
                }

                if((textStart + sourceStart) > textLength)
                    throw new IndexOutOfBoundsException();

                int available = textLength - sourceStart;
                if(available < 0){
                    throw new IndexOutOfBoundsException("sourceStart is greater than" +
                            "number of characters associated with this event");
                }

                int copiedLength = Math.min(available,length);

                System.arraycopy(base64EncodedText, getTextStart() + sourceStart , target, targetStart, copiedLength);
                textStart = sourceStart;
                return copiedLength;
            }
            return reader.getTextCharacters(sourceStart, target, targetStart, length);
        }

        public String getCharacterEncodingScheme() {
            return reader.getCharacterEncodingScheme();
        }

        public String getElementText() throws XMLStreamException {
            return reader.getElementText();
        }

        public String getEncoding() {
            return reader.getEncoding();
        }

        public String getLocalName() {
            return reader.getLocalName();
        }

        public String getNamespaceURI() {
            return reader.getNamespaceURI();
        }

        public String getPIData() {
            return reader.getPIData();
        }

        public String getPITarget() {
            return reader.getPITarget();
        }

        public String getPrefix() {
            return reader.getPrefix();
        }

        public String getText() {
            if (xopReferencePresent) {
                String text =  base64AttData.toString();
                textLength = text.length();
            }
            return reader.getText();
        }

        public String getVersion() {
            return reader.getVersion();
        }

        public String getAttributeLocalName(int index) {
            return reader.getAttributeLocalName(index);
        }

        public String getAttributeNamespace(int index) {
            return reader.getAttributeNamespace(index);
        }

        public String getAttributePrefix(int index) {
            return reader.getAttributePrefix(index);
        }

        public String getAttributeType(int index) {
            return reader.getAttributeType(index);
        }

        public String getAttributeValue(int index) {
            return reader.getAttributeValue(index);
        }

        public String getNamespacePrefix(int index) {
            return reader.getNamespacePrefix(index);
        }

        public String getNamespaceURI(int index) {
            return reader.getNamespaceURI(index);
        }

        public QName getName() {
            return reader.getName();
        }

        public QName getAttributeName(int index) {
            return reader.getAttributeName(index);
        }

        public Location getLocation() {
            return reader.getLocation();
        }

        public Object getProperty(String name) throws IllegalArgumentException {
            return reader.getProperty(name);
        }

        public void require(int type, String namespaceURI, String localName) throws XMLStreamException {
            reader.require(type, namespaceURI, localName);
        }

        public String getNamespaceURI(String prefix) {
            return reader.getNamespaceURI(prefix);
        }

        public String getAttributeValue(String namespaceURI, String localName) {
            return reader.getAttributeValue(namespaceURI, localName);
        }
    }

    private static final String XOP_CONTENT_TYPE = "application/xop+xml";
    private static final String XOP_PREF ="<Include xmlns=\"http://www.w3.org/2004/08/xop/include\" href=\"cid:";
    private static final String XOP_SUFF ="\"/>";
    private static final String XOP_LOCALNAME = "Include";
    private static final String XOP_NAMESPACEURI = "http://www.w3.org/2004/08/xop/include";
}
