package com.sun.xml.ws.sandbox.impl;

import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.pipe.Decoder;
import com.sun.xml.ws.api.message.Messages;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.encoding.xml.XMLMessage;
import com.sun.xml.ws.sandbox.message.impl.saaj.SAAJMessage;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.sandbox.message.impl.stream.StreamAttachment;

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

    public XMLHTTPDecoder() {
    }

    public void decode(InputStream in, String contentType, Packet packet) throws IOException {
        Message message = XMLMessage.create(contentType, in);
        packet.setMessage(message);
    }
    
    public Message decodeXMLMultipart(InputStream in, String contentType) throws IOException {
        XMLHTTPMultipartDecoder decoder = new XMLHTTPMultipartDecoder();
        return decoder.decode(in, contentType);
    }

    public void decode(ReadableByteChannel in, String contentType, Packet packet) {
        
        // TODO
        throw new UnsupportedOperationException();
    }

    public Decoder copy() {
        return this;
    }
    
    public static class XMLHTTPMultipartDecoder {
        
        MimeMultipartRelatedDecoder decoder;
        
        public XMLHTTPMultipartDecoder() {
            decoder = new MimeMultipartRelatedDecoder(SOAPVersion.SOAP_11);
        }
        
        public Message decode(InputStream in, String contentType) throws IOException {
            decoder.decode(in, contentType);
            // TODO need to create a Message around MimeMultipartRelatedDecoder
            return null;
            //return XMLMessage.create(decoder);
        }
        
    }

    public static final XMLHTTPDecoder INSTANCE = new XMLHTTPDecoder();
}
