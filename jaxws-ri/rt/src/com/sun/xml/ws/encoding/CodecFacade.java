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

import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.pipe.ContentType;
import com.sun.xml.ws.api.pipe.Codec;
import com.sun.xml.ws.api.pipe.Decoder;
import com.sun.xml.ws.binding.SOAPBindingImpl;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.nio.channels.WritableByteChannel;
import java.nio.channels.ReadableByteChannel;

/**
 * Codec that can handle MTOM, SwA, and plain SOAP payload.
 *
 * <p>
 * This is used when we need to determine the encoding from what we received (for decoding)
 * and from configuration and {@link Message} contents (for encoding)
 *
 * @author Vivek Pandey
 * @author Kohsuke Kawaguchi
 */
public class CodecFacade extends MimeCodec {
    private final MimeCodec mtomCodec;
    private final Codec soapCodec;
    private final MimeCodec swaCodec;
    private final SOAPBindingImpl binding;

    public CodecFacade(WSBinding binding) {
        super(binding.getSOAPVersion());
        mtomCodec = new MtomCodec(version);
        soapCodec = StreamSOAPCodec.create(version);
        swaCodec = new SwACodec(version);
        this.binding = (SOAPBindingImpl)binding;
    }

    public ContentType getStaticContentType(Packet packet) {
        return getEncoder(packet).getStaticContentType(packet);
    }

    public ContentType encode(Packet packet, OutputStream out) throws IOException {
        return getEncoder(packet).encode(packet, out);
    }

    public ContentType encode(Packet packet, WritableByteChannel buffer) {
        return getEncoder(packet).encode(packet, buffer);
    }

    public void decode(InputStream in, String contentType, Packet packet) throws IOException {
        if(isMultipartRelated(contentType))
            // parse the multipart portion and then decide whether it's MTOM or SwA
            super.decode(in, contentType, packet);
        else
            soapCodec.decode(in, contentType, packet);
    }

    public void decode(ReadableByteChannel in, String contentType, Packet packet) {
        if(isMultipartRelated(contentType))
            super.decode(in, contentType, packet);
        else
            soapCodec.decode(in, contentType, packet);
    }

    @Override
    protected void decode(MimeMultipartParser mpp, Packet packet) throws IOException {
        // is this SwA or XOP?
        if(mpp.getRootPart().getContentType().startsWith("application/xop+xml"))
            mtomCodec.decode(mpp,packet);
        else
            swaCodec.decode(mpp,packet);
    }

    private boolean isMultipartRelated(String contentType){ 
        return contentType.length() >= MULTIPART.length() && MULTIPART.equalsIgnoreCase(contentType.substring(0,MULTIPART.length()));
    }

    public CodecFacade copy() {
        return new CodecFacade(binding);
    }

    /**
     * Determines the encoding codec.
     */
    private Codec getEncoder(Packet p){
        if(binding.isMTOMEnabled())
            return mtomCodec;

        Message m = p.getMessage();
        if(m==null || m.getAttachments().isEmpty())
            return soapCodec;
        else
            return swaCodec;
    }

    private static final String MULTIPART = "multipart";
}
