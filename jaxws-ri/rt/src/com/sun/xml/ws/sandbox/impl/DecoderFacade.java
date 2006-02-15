package com.sun.xml.ws.sandbox.impl;

import com.sun.xml.ws.api.pipe.Decoder;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.SOAPVersion;

import java.io.InputStream;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;

/**
 * Delegates decoding task to {@link MimeMultipartRelatedDecoder} or {@link TestDecoderImpl} based on the content type.
 *
 * @author Vivek Pandey
 */
public class DecoderFacade implements Decoder {
    private final Decoder mimeMPSoapDecoder;
    private final Decoder soapHttpDecoder;

    //is multipart enough hint that the message is a Multipart/Related?
    private static final char[] mrIdentifier = {'m', 'u', 'l', 't', 'i', 'p', 'a', 'r', 't'};

    public DecoderFacade(SOAPVersion version) {
        mimeMPSoapDecoder = MimeMultipartRelatedDecoder.get(version);
        soapHttpDecoder = StreamSOAPDecoder.create(version);
    }

    public void decode(InputStream in, String contentType, Packet packet) throws IOException {
        if(isMultipartRelated(contentType))
            mimeMPSoapDecoder.decode(in, contentType, packet);
        else
            soapHttpDecoder.decode(in, contentType, packet);
    }

    public void decode(ReadableByteChannel in, String contentType, Packet packet) {
        if(isMultipartRelated(contentType))
            mimeMPSoapDecoder.decode(in, contentType, packet);
        else
            soapHttpDecoder.decode(in, contentType, packet);
    }

    public Decoder copy() {
        return null;
    }

    private boolean isMultipartRelated(String contentType){
        if(contentType.length() <= mrIdentifier.length)
            return false;

        String contentTypebeg = contentType.substring(0, mrIdentifier.length).toLowerCase();
        for(int i = 0; i < contentTypebeg.length(); i++){
            if(mrIdentifier[i] != contentTypebeg.charAt(i))
                return false;
        }
        return true;
    }
}
