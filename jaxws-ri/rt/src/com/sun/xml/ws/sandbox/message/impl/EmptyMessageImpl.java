package com.sun.xml.ws.sandbox.message.impl;

import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.message.Header;
import com.sun.xml.ws.api.message.HeaderList;
import com.sun.xml.ws.api.message.Message;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;

/**
 * {@link Message} that has no body.
 *
 * TODO: this is a work in progress.
 *
 * @author Kohsuke Kawaguchi
 */
public class EmptyMessageImpl extends AbstractMessageImpl {

    /**
     * If a message has no payload, it's more likely to have
     * some header, so we create it eagerly here.
     */
    private final HeaderList headers;

    public EmptyMessageImpl(SOAPVersion version) {
        super(version);
        this.headers = new HeaderList();
    }

    public EmptyMessageImpl(HeaderList headers, SOAPVersion version){
        super(version);
        if(headers==null)
            headers = new HeaderList();
        this.headers = headers;
    }


    /**
     * Copy constructor.
     */
    private EmptyMessageImpl(EmptyMessageImpl that) {
        super(that);
        this.headers = new HeaderList(that.headers);
    }

    public boolean hasHeaders() {
        return !headers.isEmpty();
    }

    public HeaderList getHeaders() {
        return headers;
    }

    public String getPayloadLocalPart() {
        return null;
    }

    public String getPayloadNamespaceURI() {
        return null;
    }

    public boolean hasPayload() {
        return false;
    }

    public Source readPayloadAsSource() {
        return null;
    }

    public SOAPMessage readAsSOAPMessage() throws SOAPException {
        SOAPMessage msg = soapVersion.saajMessageFactory.createMessage();
        for (Header header : headers) {
            header.writeTo(msg);
        }
        return msg;
    }

    public XMLStreamReader readPayload() throws XMLStreamException {
        return null;
    }

    public void writePayloadTo(XMLStreamWriter sw) throws XMLStreamException {
        // noop
    }

    public void writePayloadTo(ContentHandler contentHandler, ErrorHandler errorHandler, boolean fragment) throws SAXException {
        // noop
    }

    public Message copy() {
        return new EmptyMessageImpl(this);
    }
}
