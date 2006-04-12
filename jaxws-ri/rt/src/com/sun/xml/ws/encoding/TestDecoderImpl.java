/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License).  You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the license at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * 
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.xml.ws.encoding;

import com.sun.xml.ws.api.pipe.Decoder;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.message.saaj.SAAJMessage;
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
        factory = soapVersion.saajMessageFactory;
    }

    public void decode(InputStream in, String contentType, Packet packet) throws IOException {
        try {
            MimeHeaders headers = new MimeHeaders();
            headers.addHeader("Content-Type", contentType);
            packet.setMessage(new SAAJMessage(factory.createMessage(headers,in)));
        } catch (SOAPException e) {
            throw new WebServiceException("Unable to parse a message",e);
        }
    }

    public void decode(ReadableByteChannel in, String contentType, Packet packet) {
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
