package com.sun.xml.ws.sandbox.impl;

import com.sun.xml.ws.sandbox.Decoder;
import com.sun.xml.ws.sandbox.message.Message;
import com.sun.xml.ws.sandbox.message.impl.saaj.SAAJMessage;
import com.sun.xml.ws.encoding.soap.SOAPVersion;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPException;
import javax.xml.ws.WebServiceException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;

/**
 * Mock up {@link Decoder} that just reads the SOAP envelope as XML,
 * until we get a real {@link Decoder} implemented.
 *
 * @author Kohsuke Kawaguchi
 */
public final class TestDecoderImpl implements Decoder {

    private final MessageFactory factory;

    private TestDecoderImpl(SOAPVersion soapVersion) {
        factory = soapVersion.saajFactory;
    }

    public Message decode(InputStream in, String contentType) throws IOException {
        try {
            return new SAAJMessage(factory.createMessage(null,in));
        } catch (SOAPException e) {
            throw new WebServiceException("Unable to parse a message",e);
        }
    }

    public Message decode(ReadableByteChannel in, String contentType) {
        // TODO
        throw new UnsupportedOperationException();
    }

    public Decoder copy() {
        return this;
    }


    public static final Decoder INSTANCE11 = new TestDecoderImpl(SOAPVersion.SOAP_11);
    public static final Decoder INSTANCE12 = new TestDecoderImpl(SOAPVersion.SOAP_12);
}
