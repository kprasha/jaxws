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
    private final SOAPVersion soapVersion;

    //is multipart enough hint that the message is a Multipart/Related?
    private static final char[] mrIdentifier = {'m', 'u', 'l', 't', 'i', 'p', 'a', 'r', 't'};

    public DecoderFacade(SOAPVersion version) {
        this.soapVersion = version;
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
        return new DecoderFacade(soapVersion);
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
