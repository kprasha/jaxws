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

import com.sun.xml.ws.sandbox.message.HeaderList;
import com.sun.xml.ws.sandbox.message.Message;
import com.sun.xml.ws.sandbox.message.MessageProperties;
import com.sun.xml.ws.sandbox.message.impl.AbstractMessageImpl;
import com.sun.xml.ws.util.xml.StAXSource;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;

import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

public class StreamMessage extends AbstractMessageImpl {
    private final MessageProperties props = new MessageProperties();

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
    public StreamMessage(HeaderList headers, XMLStreamReader reader) {
        this.headers = headers;
        this.reader = reader;
        this.payloadLocalName = reader.getLocalName();
        this.payloadNamespaceURI = reader.getNamespaceURI();
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

    public MessageProperties getProperties() {
        return props;
    }

    public String getPayloadLocalPart() {
        return payloadLocalName;
    }

    public String getPayloadNamespaceURI() {
        return payloadNamespaceURI;
    }

    public Source readPayloadAsSource() {
        return new StAXSource(reader, true);
    }

    public SOAPMessage readAsSOAPMessage() {
        throw new UnsupportedOperationException();
    }

    public <T> T readPayloadAsJAXB(Unmarshaller unmarshaller) throws JAXBException {
        // TODO: How can the unmarshaller process this as a fragment?
        return (T)unmarshaller.unmarshal(reader);
    }

    public XMLStreamReader readPayload() {
        // TODO: What about access at and beyond </soap:Body>
        return this.reader;
    }



    public void writePayloadTo(XMLStreamWriter sw) {
        throw new UnsupportedOperationException();
    }

    public void writeTo(XMLStreamWriter sw) {
        throw new UnsupportedOperationException();
    }

    public void writeTo(ContentHandler contentHandler, ErrorHandler errorHandler) throws SAXException {
        // TODO
        throw new UnsupportedOperationException();
    }

    public Message copy() {
        throw new UnsupportedOperationException();
    }
}
