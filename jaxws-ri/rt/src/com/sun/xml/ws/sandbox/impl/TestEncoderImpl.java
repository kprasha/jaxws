package com.sun.xml.ws.sandbox.impl;

import com.sun.xml.ws.sandbox.Encoder;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.streaming.XMLStreamWriterFactory;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.ws.WebServiceException;
import java.io.OutputStream;
import java.nio.channels.WritableByteChannel;

/**
 * Mock up {@link Encoder} that just writes the SOAP envelope as XML,
 * until we get a real {@link Encoder} implemented.
 *
 * @author Jitu
 */
public final class TestEncoderImpl implements Encoder {
    public String getStaticContentType() {
        return "text/xml";
    }

    public String encode(Message message, OutputStream out) {
        XMLStreamWriter writer = XMLStreamWriterFactory.createXMLStreamWriter(out);
        try {
            message.writeTo(writer);
        } catch (XMLStreamException e) {
            throw new WebServiceException(e);
        }
        return "text/xml";
    }

    public String encode(Message message, WritableByteChannel buffer) {
        //TODO: not yet implemented
        throw new UnsupportedOperationException();
    }

    public Encoder copy() {
        return this;
    }

    public static final Encoder INSTANCE = new TestEncoderImpl();
}
