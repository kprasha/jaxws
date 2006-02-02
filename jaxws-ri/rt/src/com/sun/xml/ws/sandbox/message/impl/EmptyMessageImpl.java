package com.sun.xml.ws.sandbox.message.impl;

import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.HeaderList;
import com.sun.xml.ws.api.message.Packet;

import javax.xml.transform.Source;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

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

    public EmptyMessageImpl() {
        headers = new HeaderList();
    }

    /**
     * Copy constructor.
     */
    private EmptyMessageImpl(EmptyMessageImpl that) {
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
        // TODO
        throw new UnsupportedOperationException();
    }

    public SOAPMessage readAsSOAPMessage() throws SOAPException {
        // TODO
        throw new UnsupportedOperationException();
    }

    public XMLStreamReader readPayload() throws XMLStreamException {
        // TODO
        throw new UnsupportedOperationException();
    }

    public void writePayloadTo(XMLStreamWriter sw) throws XMLStreamException {
        // TODO
        throw new UnsupportedOperationException();
    }

    public void writeTo(XMLStreamWriter sw) throws XMLStreamException {
        // TODO
        throw new UnsupportedOperationException();
    }

    public void writeTo(ContentHandler contentHandler, ErrorHandler errorHandler) throws SAXException {
        // TODO
        throw new UnsupportedOperationException();
    }

    public Message copy() {
        return new EmptyMessageImpl(this);
    }
}
