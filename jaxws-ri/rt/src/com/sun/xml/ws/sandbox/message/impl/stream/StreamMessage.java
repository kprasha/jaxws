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
import com.sun.xml.ws.sandbox.impl.TagInfoset;
import com.sun.xml.ws.sandbox.message.impl.AbstractMessageImpl;
import com.sun.xml.ws.sandbox.message.impl.EmptyMessageImpl;
import com.sun.xml.ws.util.xml.DummyLocation;
import com.sun.xml.ws.util.xml.StAXSource;
import com.sun.xml.ws.util.xml.XMLStreamReaderToContentHandler;
import com.sun.xml.ws.util.xml.XMLStreamReaderToXMLStreamWriter;
import com.sun.istack.NotNull;
import com.sun.istack.Nullable;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.ws.WebServiceException;

/**
 * {@link Message} implementation backed by {@link XMLStreamReader}.
 *
 * TODO: we need another message class that keeps {@link XMLStreamReader} that points
 * at the start of the envelope element.
 */
public final class StreamMessage extends AbstractMessageImpl {
    /**
     * The reader will be positioned at
     * the first child of the SOAP body
     */
    private @NotNull XMLStreamReader reader;

    // lazily created
    private @Nullable HeaderList headers;

    private final String payloadLocalName;

    private final String payloadNamespaceURI;

    /**
     * infoset about the SOAP envelope, header, and body.
     *
     * <p>
     * If the creater of this object didn't care about those,
     * we use stock values.
     */
    private /*almost final*/ @NotNull TagInfoset envelopeTag,headerTag,bodyTag;

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
     *      points at the start element of the payload (or the end element of the &lt;s:Body>
     *      if there's no payload)
     */
    public StreamMessage(@Nullable HeaderList headers, @NotNull XMLStreamReader reader, @NotNull SOAPVersion soapVersion) {
        super(soapVersion);
        this.headers = headers;
        this.reader = reader;
        //if the reader is pointing to the end element </soapenv:Body> then its empty message
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

        // use the default infoset representation for headers
        int base = soapVersion.ordinal()*3;
        this.envelopeTag = DEFAULT_TAGS[base];
        this.headerTag = DEFAULT_TAGS[base+1];
        this.bodyTag = DEFAULT_TAGS[base+2];
    }

    /**
     * Creates a {@link StreamMessage} from a {@link XMLStreamReader}
     * and the complete infoset of the SOAP envelope.
     *
     * <p>
     * See {@link #StreamMessage(HeaderList, XMLStreamReader, SOAPVersion)} for
     * the description of the basic parameters.
     *
     * @param headerTag
     *      Null if the message didn't have a header tag.
     *
     */
    public StreamMessage(@NotNull TagInfoset envelopeTag, @Nullable TagInfoset headerTag, @Nullable HeaderList headers, @NotNull TagInfoset bodyTag, @NotNull XMLStreamReader reader, @NotNull SOAPVersion soapVersion) {
        this(headers,reader,soapVersion);
        assert envelopeTag!=null && bodyTag!=null;
        this.envelopeTag = envelopeTag;
        this.headerTag = headerTag!=null ? headerTag : DEFAULT_TAGS[soapVersion.ordinal()*3+1];
        this.bodyTag = bodyTag;
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
        if(payloadLocalName==null)
            return; // no body
        new XMLStreamReaderToXMLStreamWriter().bridge(reader,writer);
    }

    public void writeTo(XMLStreamWriter sw) throws XMLStreamException{
        writeEnvelope(sw);
    }

    /**
     * This method should be called when the StreamMessage is created with a payload
     * @param writer
     */
    private void writeEnvelope(XMLStreamWriter writer) throws XMLStreamException {
        assert unconsumed();
        envelopeTag.writeStart(writer);

        //write headers
        HeaderList hl = getHeaders();
        if(hl.size() > 0){
            headerTag.writeStart(writer);
            for(Header h:hl){
                h.writeTo(writer);
            }
            writer.writeEndElement();
        }
        bodyTag.writeStart(writer);
        if(hasPayload())
            writePayloadTo(writer);
        writer.writeEndElement();
        writer.writeEndElement();
    }

    public void writePayloadTo(ContentHandler contentHandler, ErrorHandler errorHandler) throws SAXException {
        assert unconsumed();
        try {
            if(payloadLocalName==null)
                return; // no body

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

            return new StreamMessage(envelopeTag, headerTag, HeaderList.copy(headers), bodyTag, xsb.processUsingXMLStreamReader(), soapVersion);
        } catch (XMLStreamException e) {
            throw new WebServiceException("Failed to copy a message",e);
        } catch (XMLStreamBufferException e) {
            throw new WebServiceException("Failed to copy a message",e);
        }
    }

    public void writeTo( ContentHandler contentHandler, ErrorHandler errorHandler ) throws SAXException {
        contentHandler.setDocumentLocator(NULL_LOCATOR);
        contentHandler.startDocument();
        envelopeTag.writeStart(contentHandler);
        headerTag.writeStart(contentHandler);
        if(hasHeaders()) {
            HeaderList headers = getHeaders();
            int len = headers.size();
            for( int i=0; i<len; i++ ) {
                // shouldn't JDK be smart enough to use array-style indexing for this foreach!?
                headers.get(i).writeTo(contentHandler,errorHandler);
            }
        }
        headerTag.writeEnd(contentHandler);
        bodyTag.writeStart(contentHandler);
        writePayloadTo(contentHandler,errorHandler);
        bodyTag.writeEnd(contentHandler);
        envelopeTag.writeEnd(contentHandler);

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

    /**
     * Default s:Envelope, s:Header, and s:Body tag infoset definitions.
     *
     * We need 3 for SOAP 1.1, 3 for SOAP 1.2.
     */
    private static final TagInfoset[] DEFAULT_TAGS;

    static {
        DEFAULT_TAGS = new TagInfoset[6];
        create(SOAPVersion.SOAP_11);
        create(SOAPVersion.SOAP_12);
    }

    private static void create(SOAPVersion v) {
        int base = SOAPVersion.SOAP_11.ordinal()*3;
        DEFAULT_TAGS[base  ] = new TagInfoset(v.nsUri,"Envelope","S",EMPTY_ATTS,"S",v.nsUri);
        DEFAULT_TAGS[base+1] = new TagInfoset(v.nsUri,"Header","S",EMPTY_ATTS);
        DEFAULT_TAGS[base+2] = new TagInfoset(v.nsUri,"Body","S",EMPTY_ATTS);
    }
}
