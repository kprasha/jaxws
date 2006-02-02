package com.sun.xml.ws.sandbox.impl;

import com.sun.xml.ws.api.pipe.Encoder;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.SOAPVersion;
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

    private final String contentType;

    private TestEncoderImpl(String contentType) {
        this.contentType = contentType;
    }

    public String getStaticContentType() {
        return contentType;
    }

    public String encode(Packet packet, OutputStream out) {
        if (packet.getMessage() != null) {
            XMLStreamWriter writer = XMLStreamWriterFactory.createXMLStreamWriter(out);
            try {
                packet.getMessage().writeTo(writer);
            } catch (XMLStreamException e) {
                throw new WebServiceException(e);
            }
        }
        return contentType;
    }

    public String encode(Packet packet, WritableByteChannel buffer) {
        //TODO: not yet implemented
        throw new UnsupportedOperationException();
    }

    public Encoder copy() {
        return this;
    }

    public static final Encoder INSTANCE11 = new TestEncoderImpl("text/xml");
    public static final Encoder INSTANCE12 = new TestEncoderImpl("application/soap+xml");

    public static Encoder get(SOAPVersion version) {
        if(version==null)
            // this decoder is for SOAP, not for XML/HTTP
            throw new IllegalArgumentException();
        switch(version) {
        case SOAP_11:
            return INSTANCE11;
        case SOAP_12:
            return INSTANCE12;
        default:
            throw new AssertionError();
        }
    }
}
