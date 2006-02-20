package com.sun.xml.ws.sandbox.impl;

import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.Decoder;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.sandbox.message.impl.stream.StreamAttachment;
import com.sun.xml.ws.streaming.XMLStreamReaderFactory;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.Location;
import javax.xml.ws.WebServiceException;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;

import java.io.InputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.channels.ReadableByteChannel;
import java.util.Iterator;
import java.util.Arrays;
import java.net.URLDecoder;

import org.jvnet.staxex.XMLStreamReaderEx;
import org.jvnet.staxex.NamespaceContextEx;
import org.jvnet.staxex.Base64Data;

/**
 * @author Vivek Pandey
 */
public class MtomDecoder implements Decoder{
    private final StreamSOAPDecoder decoder;
    private final MimeMultipartRelatedDecoder mimeMultipartDecoder;
    private static final String XOP_LOCALNAME = "Include";
    private static final String XOP_NAMESPACEURI = "http://www.w3.org/2004/08/xop/include";
    private final MtomXMLStreamReaderEx xmlStreamReaderEx = new MtomXMLStreamReaderEx();
    private XMLStreamReader reader;
    private Base64Data base64AttData;
    private boolean xopReferencePresent = false;

    //values that will set to whether mtom or not as caller can call getPcData or getTextCharacters
    private int textLength;
    private int textStart;

    //To be used with #getTextCharacters
    private char[] base64EncodedText;

    public MtomDecoder(MimeMultipartRelatedDecoder mimeMultipartDecoder, SOAPVersion soapVersion) {
        this.decoder = (StreamSOAPDecoder)StreamSOAPDecoder.create(soapVersion);
        this.mimeMultipartDecoder = mimeMultipartDecoder;
    }

    public void decode(InputStream in, String contentType, Packet packet) throws IOException {
        reader = XMLStreamReaderFactory.createXMLStreamReader(in, true);
        packet.setMessage(decoder.decode(getXmlStreamReaderEx(), contentType));
    }

    public void decode(ReadableByteChannel in, String contentType, Packet packet) {
        decoder.decode(in,contentType,packet);
    }

    public Decoder copy() {
        return decoder.copy();
    }

     public XMLStreamReaderEx getXmlStreamReaderEx() {
        return xmlStreamReaderEx;
    }

    private CharSequence getMtomPCData() {
        if(xopReferencePresent){
            return base64AttData;
        }
        return reader.getText();
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

        public int next() throws XMLStreamException {
            int event = reader.next();
            if ((event == XMLStreamConstants.START_ELEMENT) && reader.getLocalName().equals(XOP_LOCALNAME) && reader.getNamespaceURI().equals(XOP_NAMESPACEURI))
            {
                //its xop reference, take the URI reference
                String href = reader.getAttributeValue(null, "href");
                href = decodeCid(href);
                try {
                    StreamAttachment att = mimeMultipartDecoder.getMIMEPart(href);
                    base64AttData = new Base64Data();
                    base64AttData.set(att.asByteArray(), att.asByteArray().length, att.getContentType());
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
                char[] chars = new char[(base64AttData.getDataLen()+2)*4/3];
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
                    base64EncodedText = new char[(base64AttData.getDataLen()+2)*4/3];
                    base64AttData.writeTo(base64EncodedText, 0);
                    textLength = base64EncodedText.length;
                    textStart = 0;
                }

                if((textStart + sourceStart) > textLength)
                    throw new IndexOutOfBoundsException();

                int copiedLength = 0;
                int available = textLength - sourceStart;
                if(available < 0){
                    throw new IndexOutOfBoundsException("sourceStart is greater than" +
                            "number of characters associated with this event");
                }
                if(available < length){
                    copiedLength = available;
                } else{
                    copiedLength = length;
                }

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
}
