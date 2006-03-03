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
package com.sun.xml.ws.sandbox.message.impl.stream;

import com.sun.xml.bind.api.Bridge;
import com.sun.xml.bind.api.BridgeContext;
import com.sun.xml.stream.buffer.XMLStreamBuffer;
import com.sun.xml.stream.buffer.XMLStreamBufferException;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.message.Header;
import com.sun.xml.ws.api.message.HeaderList;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.sandbox.message.impl.AbstractMessageImpl;
import com.sun.xml.ws.sandbox.message.impl.EmptyMessageImpl;
import com.sun.xml.ws.util.xml.DummyLocation;
import com.sun.xml.ws.util.xml.StAXSource;
import com.sun.xml.ws.util.xml.XMLStreamReaderToContentHandler;
import com.sun.xml.ws.util.xml.XMLStreamReaderToXMLStreamWriter;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.ws.WebServiceException;
import java.util.Map;

public class StreamMessage extends AbstractMessageImpl {
    /**
     * flag that tells whether StreamMessage is created with the
     * XMLStreamReader that points to the payload or soapenv:Envelop
     *
     * Probably we may need different classes for payload and Envelope.
     * Lets keep this way for the timebeing.
     */
    private boolean isPayload;

    private String bodyQname ="";
    private String envQname ="";
    private String headerQname ="";


    /**
     * The reader will be positioned at
     * the first child of the SOAP body
     */
    protected XMLStreamReader reader;

    // lazily created
    private HeaderList headers;

    private final String payloadLocalName;

    private final String payloadNamespaceURI;
    Map<String,String> soapHdrNSDecls;
    Map<String,String> bodyNSDecls;
    Map<String,String> envNSDecls;
    Attributes shAttrs ;
    Attributes bodyAttrs ;
    Attributes envAttrs ;
    private QName envelope;
    private QName soapHeader;
    private QName body;

    /**
     * Creates a {@link StreamMessage} from a {@link XMLStreamReader}
     * that points at the start element of the payload, and headers.
     *
     * <p>
     * This method creaets a {@link Message} from a payload.
     *
     * @param headers
     *      if null, it means no headers. if non-null,
     *      it will be owned by this message.
     * @param reader
     *      must not be null.
     */
    public StreamMessage(HeaderList headers, XMLStreamReader reader, SOAPVersion soapVersion) {
        super(soapVersion);
        this.headers = headers;
        this.reader = reader;
        isPayload = true;
        //if the reader is pointing to the end element <soapenv:Body/> then its empty message
        // or no payload
        if(reader.getEventType() == javax.xml.stream.XMLStreamConstants.END_ELEMENT){
            String body = reader.getLocalName();
            String nsUri = reader.getNamespaceURI();
            assert body != null;
            assert nsUri != null;
            //if its not soapenv:Body then throw exception, we received malformed stream
            if(body.equals("Body") && nsUri.equals(soapVersion.nsUri)){
                this.payloadLocalName = null;
                this.payloadNamespaceURI = null;
            }else{ //TODO: i18n and also we should be throwing better message that this
                throw new WebServiceException("Malformed stream: {"+nsUri+"}"+body);
            }
        }else{
            this.payloadLocalName = reader.getLocalName();
            this.payloadNamespaceURI = reader.getNamespaceURI();
        }
    }

    public StreamMessage(QName soapEnvelope,Map<String,String> envNSDecl,Attributes envAttrs,QName soapHeader,Map<String,String> headerNSDecl,Attributes shAttrs,HeaderList headers, XMLStreamReader reader,QName soapBody,Map<String,String>bodyNSDecls,Attributes bodyAttrs, SOAPVersion soapVersion) {
        super(soapVersion);
        this.headers = headers;
        this.reader = reader;
        this.soapHdrNSDecls = headerNSDecl;
        this.envNSDecls = envNSDecl;
        this.envAttrs = envAttrs;
        this.shAttrs = shAttrs;
        this.bodyAttrs = bodyAttrs;
        this.bodyNSDecls = bodyNSDecls;
        this.body = soapBody;
        this.envelope = soapEnvelope;
        this.soapHeader = soapHeader;
        if(this.envAttrs == null){
            this.envAttrs = EMPTY_ATTS;
        }


        if(this.shAttrs == null){
            this.shAttrs = EMPTY_ATTS;
        }
        isPayload = true;
        //if the reader is pointing to the end element <soapenv:Body/> then its empty message
        // or no payload
        if(reader.getEventType() == javax.xml.stream.XMLStreamConstants.END_ELEMENT){
            String body = reader.getLocalName();
            String nsUri = reader.getNamespaceURI();
            assert body != null;
            assert nsUri != null;
            //if its not soapenv:Body then throw exception, we received malformed stream
            if(body.equals("Body") && nsUri.equals(soapVersion.nsUri)){
                this.payloadLocalName = null;
                this.payloadNamespaceURI = null;
            }else{ //TODO: i18n and also we should be throwing better message that this
                throw new WebServiceException("Malformed stream: {"+nsUri+"}"+body);
            }
        }else{
            this.payloadLocalName = reader.getLocalName();
            this.payloadNamespaceURI = reader.getNamespaceURI();
        }
    }


    /**
     * Creates a {@link StreamMessage}  from a {@link XMLStreamReader}
     * that points at the start element of &lt;S:Envelope>.
     *
     * <p>
     * This method creates a message from a complete message,
     * and parses headers and so on within itself.
     *
     * @param reader
     *      must not be null.
     */
    public StreamMessage(XMLStreamReader reader) {
        // TODO: implement this method later
        super((SOAPVersion)null);
        throw new UnsupportedOperationException();
    }

    public boolean hasHeaders() {
        return headers!=null && !headers.isEmpty();
    }

    public HeaderList getHeaders() {
        if (headers == null) {
            headers = new HeaderList();
        }
        return headers;
    }

    public String getPayloadLocalPart() {
        return payloadLocalName;
    }

    public String getPayloadNamespaceURI() {
        return payloadNamespaceURI;
    }

    public boolean hasPayload() {
        return payloadLocalName!=null;
    }

    public Source readPayloadAsSource() {
        return new StAXSource(reader, true);
    }

    public Object readPayloadAsJAXB(Unmarshaller unmarshaller) throws JAXBException {
        if(!hasPayload())
            return null;
        assert unconsumed();
        // TODO: How can the unmarshaller process this as a fragment?
        return unmarshaller.unmarshal(reader);
    }

    public <T> T readPayloadAsJAXB(Bridge<T> bridge, BridgeContext context) throws JAXBException {
        if(!hasPayload())
            return null;
        assert unconsumed();
        return bridge.unmarshal(context,reader);
    }

    public XMLStreamReader readPayload() {
        // TODO: What about access at and beyond </soap:Body>
        assert unconsumed();
        return this.reader;
    }

    public void writePayloadTo(XMLStreamWriter writer)throws XMLStreamException {
        assert unconsumed();
        new XMLStreamReaderToXMLStreamWriter().bridge(reader,writer);
    }

    public void writeTo(XMLStreamWriter sw) throws XMLStreamException{
        if(isPayload){
            writeEnvelope(sw);
        }else{
            // TODO: implement this method later
            throw new UnsupportedOperationException();
        }
    }

    /**
     * This method should be called when the StreamMessage is created with a payload
     * @param writer
     */
    private void writeEnvelope(XMLStreamWriter writer) throws XMLStreamException {
        assert unconsumed();
        writer.writeStartElement("soapenv", "Envelope", soapVersion.nsUri);
        writer.writeNamespace("soapenv", soapVersion.nsUri);
        //TODO: collect all the namespaces from the payload and add to Envelope element

        //write headers
        HeaderList hl = getHeaders();
        if(hl.size() > 0){
            writer.writeStartElement("soapenv", "Header", soapVersion.nsUri);
            for(Header h:hl){
                h.writeTo(writer);
            }
            writer.writeEndElement();
        }
        writer.writeStartElement("soapenv", "Body", soapVersion.nsUri);
        if(hasPayload())
            writePayloadTo(writer);
        writer.writeEndElement();
        writer.writeEndElement();
    }

    public void writePayloadTo(ContentHandler contentHandler, ErrorHandler errorHandler) throws SAXException {
        assert unconsumed();
        try {
            XMLStreamReaderToContentHandler conv =
                new XMLStreamReaderToContentHandler(reader,contentHandler,true);
            conv.bridge();
            reader.close();
        } catch (XMLStreamException e) {
            Location loc = e.getLocation();
            if(loc==null)   loc = DummyLocation.INSTANCE;

            SAXParseException x = new SAXParseException(
                e.getMessage(),loc.getPublicId(),loc.getSystemId(),loc.getLineNumber(),loc.getColumnNumber(),e);
            errorHandler.error(x);
        }
    }

    public Message copy() {
        // if the reader is on END element means its empty body or no payload, so lets
        // return the same message
        if(!hasPayload())
            return new EmptyMessageImpl(HeaderList.copy(headers), soapVersion);

        assert unconsumed();
        try {
            // copy the payload
            XMLStreamBuffer xsb = new XMLStreamBuffer();
            xsb.createFromXMLStreamReader(reader);

            reader = xsb.processUsingXMLStreamReader();

            return new StreamMessage(HeaderList.copy(headers), xsb.processUsingXMLStreamReader(), soapVersion);
        } catch (XMLStreamException e) {
            throw new WebServiceException("Failed to copy a message",e);
        } catch (XMLStreamBufferException e) {
            throw new WebServiceException("Failed to copy a message",e);
        }
    }

    public void writeTo( ContentHandler contentHandler, ErrorHandler errorHandler ) throws SAXException {
        contentHandler.setDocumentLocator(NULL_LOCATOR);
        contentHandler.startDocument();
        writeEnvelopeandHeader(contentHandler);
        if(hasHeaders()) {
            HeaderList headers = getHeaders();
            int len = headers.size();
            for( int i=0; i<len; i++ ) {
                // shouldn't JDK be smart enough to use array-style indexing for this foreach!?
                headers.get(i).writeTo(contentHandler,errorHandler);
            }
        }
        writeEndHeader(contentHandler);
        writeBodyTag(contentHandler);
        // write the body

        writePayloadTo(contentHandler,errorHandler);
        writeEndEnvelope(contentHandler);

    }

    protected void writeBodyTag(ContentHandler contentHandler) throws SAXException{
        StringBuilder sb = new StringBuilder();
        if(bodyNSDecls != null){
            for(String key: bodyNSDecls.keySet()){
                contentHandler.startPrefixMapping(key, bodyNSDecls.get(key));
            }
        }

        String qname ="";
        if(body.getPrefix().length()>0){
            sb.append(body.getPrefix());
            sb.append(":");
            sb.append(body.getLocalPart());
            qname = sb.toString();
        }


        bodyQname = qname;
        contentHandler.startElement(body.getNamespaceURI(),body.getLocalPart(),qname, EMPTY_ATTS);//bodyAttrs

    }



    protected void writeEndEnvelope(ContentHandler contentHandler) throws SAXException{
        contentHandler.endElement(body.getNamespaceURI(),body.getLocalPart(),bodyQname);
        if(bodyNSDecls != null){
            for(String key: bodyNSDecls.keySet()){
                contentHandler.endPrefixMapping(key);
            }
        }
        contentHandler.endElement(envelope.getNamespaceURI(),envelope.getLocalPart(),envQname);
        if(envNSDecls != null){
            for(String key: envNSDecls.keySet()){
                contentHandler.endPrefixMapping(key);
            }
        }
    }

    protected void writeEndHeader(ContentHandler contentHandler) throws SAXException{
        contentHandler.endElement(soapHeader.getNamespaceURI(),soapHeader.getLocalPart(),headerQname);
        if(soapHdrNSDecls != null){
            for(String key: soapHdrNSDecls.keySet()){
                contentHandler.endPrefixMapping(key);
            }
        }
    }

    protected void writeEnvelopeandHeader(ContentHandler contentHandler) throws SAXException{
        if(envelope != null){

            StringBuilder sb = new StringBuilder();
            if(envNSDecls != null){
                for(String key: envNSDecls.keySet()){
                    contentHandler.startPrefixMapping(key, envNSDecls.get(key));
                }
            }

            String qname ="";
            if(envelope.getPrefix().length()>0){
                sb.append(envelope.getPrefix());

                sb.append(":");
                sb.append(envelope.getLocalPart());
                qname = sb.toString();
            }
            envQname = qname;
            contentHandler.startElement(envelope.getNamespaceURI(),envelope.getLocalPart(),qname, envAttrs);
            sb.setLength(0);
            qname ="";
            if(soapHeader.getPrefix().length()>0){
                sb.append(soapHeader.getPrefix());
                sb.append(":");
                sb.append(soapHeader.getLocalPart());
                qname = sb.toString();
            }
            headerQname = qname;

            if(soapHdrNSDecls != null){
                for(String key: soapHdrNSDecls.keySet()){
                    contentHandler.startPrefixMapping(key, soapHdrNSDecls.get(key));
                }
            }
            contentHandler.startElement(soapHeader.getNamespaceURI(),soapHeader.getLocalPart(),qname,shAttrs);
        }else{
            String soapNsUri = soapVersion.nsUri;
            contentHandler.startPrefixMapping("S",soapNsUri);
            contentHandler.startElement(soapNsUri,"Envelope","S:Envelope",EMPTY_ATTS);
            contentHandler.startElement(soapNsUri,"Header","S:Header",EMPTY_ATTS);
        }
    }

    /**
     * Used for an assertion. Returns true when the message is unconsumed.
     */
    private boolean unconsumed() {
        if(reader.getEventType()!=XMLStreamReader.START_ELEMENT) {
            AssertionError error = new AssertionError("StreamMessage has been already consumed. See the nested exception for where it's consumed");
            error.initCause(consumedAt);
            throw error;
        }
        consumedAt = new Exception().fillInStackTrace();
        return true;
    }

    /**
     * Used only for debugging. This records where the message was consumed.
     */
    private Throwable consumedAt;
}
