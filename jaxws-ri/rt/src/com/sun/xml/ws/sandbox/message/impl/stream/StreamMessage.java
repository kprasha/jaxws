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
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.message.Header;
import com.sun.xml.ws.api.message.HeaderList;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.sandbox.message.impl.AbstractMessageImpl;
import com.sun.xml.ws.sandbox.message.impl.EmptyMessageImpl;
import com.sun.xml.ws.util.xml.StAXSource;
import com.sun.xml.ws.streaming.XMLStreamWriterFactory;
import com.sun.xml.ws.streaming.XMLStreamReaderFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.ws.WebServiceException;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;

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
    protected final XMLStreamReader reader;

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

    public SOAPMessage readAsSOAPMessage() {
        throw new UnsupportedOperationException();
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
        int state;
        boolean firsttime = true;
        do {
            if(firsttime){
                state = reader.getEventType();
                firsttime = false;
            }else{
                state = reader.next();
            }
            switch (state) {
                case XMLStreamConstants.START_ELEMENT:
                    /*
                     * TODO: Is this necessary, shouldn't zephyr return "" instead of
                     * null for getNamespaceURI() and getPrefix()?
                     */
                    String uri = reader.getNamespaceURI();
                    String prefix = reader.getPrefix();
                    String localName = reader.getLocalName();

                    if (prefix == null) {
                        if (uri == null) {
                            writer.writeStartElement(localName);
                        } else {
                            writer.writeStartElement(uri, localName);
                        }
                    } else {
                        assert uri != null;

                        if(prefix.length() > 0){
                            /**
                             * Before we write the
                             */
                            String writerURI = null;
                            if (writer.getNamespaceContext() != null)
                                writerURI = writer.getNamespaceContext().getNamespaceURI(prefix);
                            String writerPrefix = writer.getPrefix(uri);
                            if(declarePrefix(prefix, uri, writerPrefix, writerURI)){
                                writer.writeStartElement(prefix, localName, uri);
                                writer.setPrefix(prefix, uri != null ? uri : "");
                                writer.writeNamespace(prefix, uri);
                            }else{
                                writer.writeStartElement(prefix, localName, uri);
                            }
                        }else{
                            writer.writeStartElement(prefix, localName, uri);
                        }
                    }

                    int n = reader.getNamespaceCount();
                    // Write namespace declarations
                    for (int i = 0; i < n; i++) {
                        String nsPrefix = reader.getNamespacePrefix(i);
                        if (nsPrefix == null) nsPrefix = "";
                        // StAX returns null for default ns
                        String writerURI = null;
                        if (writer.getNamespaceContext() != null)
                            writerURI = writer.getNamespaceContext().getNamespaceURI(nsPrefix);

                        // Zephyr: Why is this returning null?
                        // Compare nsPrefix with prefix because of [1] (above)
                        String readerURI = reader.getNamespaceURI(i);

                        /**
                         * write the namespace in 3 conditions
                         *  - when the namespace URI is not bound to the prefix in writer(writerURI == 0)
                         *  - when the readerPrefix and writerPrefix are ""
                         *  - when readerPrefix and writerPrefix are not equal and the URI bound to them
                         *    are different
                         */
                        if (writerURI == null || ((nsPrefix.length() == 0) || (prefix.length() == 0)) ||
                                (!nsPrefix.equals(prefix) && !writerURI.equals(readerURI))) {
                            writer.setPrefix(nsPrefix, readerURI != null ? readerURI : "");
                            writer.writeNamespace(nsPrefix, readerURI != null ? readerURI : "");
                        }
                    }

                    // Write attributes
                    n = reader.getAttributeCount();
                    for (int i = 0; i < n; i++) {
                        String attrPrefix = reader.getAttributePrefix(i);
                        String attrURI = reader.getAttributeNamespace(i);

                        writer.writeAttribute(attrPrefix != null ? attrPrefix : "",
                            attrURI != null ? attrURI : "",
                            reader.getAttributeLocalName(i),
                            reader.getAttributeValue(i));
                        // if the attribute prefix is undeclared in current writer scope then declare it
                        setUndeclaredPrefix(attrPrefix, attrURI, writer);
                    }
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    writer.writeEndElement();
                    break;
                case XMLStreamConstants.CHARACTERS:
                    writer.writeCharacters(reader.getText());
            }
        } while (state != XMLStreamConstants.END_DOCUMENT);
        reader.close();

    }

    /**
     * sets undeclared prefixes on the writer
     * @param prefix
     * @param writer
     * @throws XMLStreamException
     */
    private void setUndeclaredPrefix(String prefix, String readerURI, XMLStreamWriter writer) throws XMLStreamException {
        String writerURI = null;
        if (writer.getNamespaceContext() != null)
            writerURI = writer.getNamespaceContext().getNamespaceURI(prefix);

        if (writerURI == null) {
            writer.setPrefix(prefix, readerURI != null ? readerURI : "");
            writer.writeNamespace(prefix, readerURI != null ? readerURI : "");
        }
    }

    /**
     * check if we need to declare
     * @param rPrefix
     * @param rUri
     * @param wPrefix
     * @param wUri
     */
    private boolean declarePrefix(String rPrefix, String rUri, String wPrefix, String wUri){
        if (wUri == null ||((wPrefix != null) && !rPrefix.equals(wPrefix))||
                (rUri != null && !wUri.equals(rUri)))
            return true;
        return false;
    }

    public void writeTo(XMLStreamWriter sw) throws XMLStreamException{
        if(isPayload){
            writeEnvelope(sw);
        }else{
            //TODO
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
        if(!hasPayload())
            writePayloadTo(writer);
        writer.writeEndElement();
        writer.writeEndElement();
    }

    public void writePayloadTo(ContentHandler contentHandler, ErrorHandler errorHandler) throws SAXException {
        throw new UnsupportedOperationException();
    }

    public Message copy() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        // if the reader is on END element means its empty body or no payload, so lets
        // return the same message
        if(!hasPayload())
            return new EmptyMessageImpl(headers, soapVersion);

        XMLStreamWriter writer = XMLStreamWriterFactory.createXMLStreamWriter(bos);
        try {
            writeTo(writer);
        } catch (XMLStreamException e) {
            throw new WebServiceException(e);
        }
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        return new StreamMessage(headers, XMLStreamReaderFactory.createXMLStreamReader(bis, true), soapVersion);

    }


}
