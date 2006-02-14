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

public class StreamMessage extends AbstractMessageImpl {
    /**
     * flag that tells whether StreamMessage is created with the
     * XMLStreamReader that points to the payload or soapenv:Envelop
     *
     * Probably we may need different classes for payload and Envelope.
     * Lets keep this way for the timebeing.
     */
    private boolean isPayload;

    /**
     * The reader will be positioned at
     * the first child of the SOAP body
     */
    protected XMLStreamReader reader;

    // lazily created
    private HeaderList headers;

    private final String payloadLocalName;

    private final String payloadNamespaceURI;

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
        // TODO: How can the unmarshaller process this as a fragment?
        return unmarshaller.unmarshal(reader);
    }

    public <T> T readPayloadAsJAXB(Bridge<T> bridge, BridgeContext context) throws JAXBException {
        if(!hasPayload())
            return null;
        return bridge.unmarshal(context,reader);
    }

    public XMLStreamReader readPayload() {
        // TODO: What about access at and beyond </soap:Body>
        return this.reader;
    }

    public void writePayloadTo(XMLStreamWriter writer)throws XMLStreamException {
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
            return new EmptyMessageImpl(headers.copy(), soapVersion);

        try {
            // copy the payload
            XMLStreamBuffer xsb = new XMLStreamBuffer();
            xsb.createFromXMLStreamReader(reader);

            reader = xsb.processUsingXMLStreamReader();

            return new StreamMessage(headers.copy(), xsb.processUsingXMLStreamReader(), soapVersion);
        } catch (XMLStreamException e) {
            throw new WebServiceException("Failed to copy a message",e);
        } catch (XMLStreamBufferException e) {
            throw new WebServiceException("Failed to copy a message",e);
        }
    }
}
