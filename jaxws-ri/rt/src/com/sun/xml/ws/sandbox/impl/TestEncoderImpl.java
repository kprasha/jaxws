package com.sun.xml.ws.sandbox.impl;

import com.sun.xml.ws.sandbox.Encoder;
import com.sun.xml.ws.sandbox.XMLStreamWriterEx;
import com.sun.xml.ws.sandbox.message.Message;
import com.sun.xml.ws.streaming.XMLStreamWriterFactory;

import java.nio.channels.WritableByteChannel;
import java.io.OutputStream;
import java.io.IOException;

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

    public String encode(Message message, OutputStream out) throws IOException {
        XMLStreamWriterEx writer = new XMLStreamWriterExImpl(XMLStreamWriterFactory.createXMLStreamWriter(out));
        message.writeTo(writer);
        return "text/xml";
    }

    public String encode(Message message, WritableByteChannel buffer) {
        //TODO: not yet implemented
        throw new UnsupportedOperationException();
    }

    public static final Encoder INSTANCE = new TestEncoderImpl();
}
