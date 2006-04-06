package com.sun.xml.ws.sandbox.message.impl.source;

import com.sun.istack.NotNull;
import com.sun.istack.Nullable;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.message.HeaderList;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.sandbox.message.impl.AbstractMessageImpl;
import com.sun.xml.ws.sandbox.message.impl.stream.StreamMessage;
import com.sun.xml.ws.streaming.SourceReaderFactory;
import com.sun.xml.ws.streaming.XMLStreamReaderUtil;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;

/**
 * {@link Message} backed by {@link Source}
 *
 * @author Vivek Pandey
 */
public class PayloadSourceMessage extends AbstractMessageImpl {
    private final StreamMessage message;
    private final Source payload;

    public PayloadSourceMessage(@Nullable HeaderList headers, @NotNull Source payload, @NotNull SOAPVersion soapVersion) {
        super(soapVersion);
        this.payload = payload;
        XMLStreamReader reader = SourceReaderFactory.createSourceReader(payload, true);
        XMLStreamReaderUtil.next(reader);
        message = new StreamMessage(headers, reader, soapVersion);
    }

    public PayloadSourceMessage(Source s, SOAPVersion soapVer) {
        this(null, s, soapVer);
    }

    public boolean hasHeaders() {
        return message.hasHeaders();
    }

    public HeaderList getHeaders() {
        return message.getHeaders();
    }

    public String getPayloadLocalPart() {
        return message.getPayloadLocalPart();
    }

    public String getPayloadNamespaceURI() {
        return message.getPayloadNamespaceURI();
    }

    public boolean hasPayload() {
        return true;
    }

    public Source readPayloadAsSource() {
        return payload;
    }

    public XMLStreamReader readPayload() throws XMLStreamException {
        return message.readPayload();
    }

    public void writePayloadTo(XMLStreamWriter sw) throws XMLStreamException {
        message.writePayloadTo(sw);
    }

    public void writeTo(ContentHandler contentHandler, ErrorHandler errorHandler) throws SAXException {
        message.writeTo(contentHandler, errorHandler);
    }

    protected void writePayloadTo(ContentHandler contentHandler, ErrorHandler errorHandler) throws SAXException {
        message.writePayloadTo(contentHandler, errorHandler);
    }

    public Message copy() {
        return message.copy();
    }
}
