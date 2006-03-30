package com.sun.xml.ws.sandbox.impl;

import com.sun.xml.ws.api.pipe.Decoder;
import com.sun.xml.ws.api.message.Messages;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.sandbox.message.impl.saaj.SAAJMessage;
import com.sun.xml.ws.api.SOAPVersion;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.WebServiceException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;

/**
 * {@link Decoder} that just reads XML/HTTP binding messages,
 *
 * @author Jitendra Kotamraju
 */
public final class XMLHTTPDecoder implements Decoder {

    private XMLHTTPDecoder() {
    }

    public void decode(InputStream in, String contentType, Packet packet) throws IOException {
        MimeHeaders headers = new MimeHeaders();
        headers.addHeader("Content-Type", contentType);
        packet.setMessage(Messages.createUsingPayload(new StreamSource(in), SOAPVersion.SOAP_11));
    }

    public void decode(ReadableByteChannel in, String contentType, Packet packet) {
        // TODO
        throw new UnsupportedOperationException();
    }

    public Decoder copy() {
        return this;
    }

    public static final Decoder INSTANCE = new XMLHTTPDecoder();
}
