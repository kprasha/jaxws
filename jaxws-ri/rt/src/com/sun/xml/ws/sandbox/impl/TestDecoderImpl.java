package com.sun.xml.ws.sandbox.impl;

import com.sun.xml.ws.api.pipe.Decoder;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.sandbox.message.impl.saaj.SAAJMessage;
import com.sun.xml.ws.api.SOAPVersion;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
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
            MimeHeaders headers = new MimeHeaders();
            headers.addHeader("Content-Type", contentType);
            return new SAAJMessage(factory.createMessage(headers,in));
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

    public static Decoder get(SOAPVersion version) {
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
