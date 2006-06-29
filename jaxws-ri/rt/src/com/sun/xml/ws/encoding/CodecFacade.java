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

import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.pipe.ContentType;
import com.sun.xml.ws.api.pipe.Codec;
import com.sun.xml.ws.binding.SOAPBindingImpl;
import com.sun.xml.ws.client.ContentNegotiation;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
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
    private static final String MULTIPART = "multipart";
    
    private static final String FASTINFOSET = "fastinfoset";
    
    private boolean _useFastInfosetForEncoding;
    
    // The XML SOAP codec
    private final StreamSOAPCodec xmlSoapCodec;
    
    // The Fast Infoset SOAP codec
    private final Codec fiSoapCodec;
    
    // The XML MTOM codec
    private final MimeCodec xmlMtomCodec;
    
    // The XML SWA codec
    private final MimeCodec xmlSwaCodec;
    
    private final SOAPBindingImpl binding;
    
    private class AcceptContentType implements ContentType {
        private ContentType _c;
        private String _accept;
        
        public AcceptContentType set(Packet p, ContentType c) {
            // TODO: need to compose based on underlying codecs
            if (p.contentNegotiation != ContentNegotiation.none) {
                _accept = "application/fastinfoset, text/xml";
            } else {
                _accept = null;
            }
            _c = c;
            return this;
        }
        
        public String getContentType() {
            return _c.getContentType();
        }
        
        public String getSOAPActionHeader() {
            return _c.getContentType();
        }
        
        public String getAcceptHeader() {
            return _accept;
        }
    }
    
    private AcceptContentType _adaptingContentType = new AcceptContentType();
    
    public CodecFacade(WSBinding binding) {
        super(binding.getSOAPVersion());
        
        xmlSoapCodec = StreamSOAPCodec.create(version);
        fiSoapCodec = getFICodec(version);
        
        xmlMtomCodec = new MtomCodec(version, xmlSoapCodec);
        
        xmlSwaCodec = new SwACodec(version, xmlSoapCodec);
        
        this.binding = (SOAPBindingImpl)binding;
    }
    
    public ContentType getStaticContentType(Packet packet) {
        ContentType toAdapt = getEncoder(packet).getStaticContentType(packet);
        return (toAdapt != null) ? _adaptingContentType.set(packet, toAdapt) : null;
    }
    
    public ContentType encode(Packet packet, OutputStream out) throws IOException {
        return _adaptingContentType.set(packet, getEncoder(packet).encode(packet, out));
    }
    
    public ContentType encode(Packet packet, WritableByteChannel buffer) {
        return _adaptingContentType.set(packet, getEncoder(packet).encode(packet, buffer));
    }
    
    public void decode(InputStream in, String contentType, Packet packet) throws IOException {
        if(isMultipartRelated(contentType))
            // parse the multipart portion and then decide whether it's MTOM or SwA
            super.decode(in, contentType, packet);
        else if(isFastInfoset(contentType)) {
            if (fiSoapCodec == null) {
                // TODO: use correct error message
                throw new RuntimeException("Fast Infoset Runtime not present");
            }
            
            // TODO ensure fast infoset is accepted
            _useFastInfosetForEncoding = true;
            fiSoapCodec.decode(in, contentType, packet);
        } else
            xmlSoapCodec.decode(in, contentType, packet);
        
        if (!_useFastInfosetForEncoding) {
            _useFastInfosetForEncoding = isFastInfosetAcceptable(packet.acceptableMimeTypes);
        }
    }
    
    public void decode(ReadableByteChannel in, String contentType, Packet packet) {
        if(isMultipartRelated(contentType))
            super.decode(in, contentType, packet);
        else if(isFastInfoset(contentType)) {
            if (fiSoapCodec == null) {
                // TODO: use correct error message
                throw new RuntimeException("Fast Infoset Runtime not present");
            }
            
            _useFastInfosetForEncoding = true;
            fiSoapCodec.decode(in, contentType, packet);
        } else
            xmlSoapCodec.decode(in, contentType, packet);
        
        if (!_useFastInfosetForEncoding) {
            _useFastInfosetForEncoding = isFastInfosetAcceptable(packet.acceptableMimeTypes);
        }
    }
    
    public CodecFacade copy() {
        return new CodecFacade(binding);
    }
    
    
    @Override
    protected void decode(MimeMultipartParser mpp, Packet packet) throws IOException {
        // is this SwA or XOP?
        if(mpp.getRootPart().getContentType().startsWith("application/xop+xml"))
            xmlMtomCodec.decode(mpp,packet);
        else
            xmlSwaCodec.decode(mpp,packet);
    }
    
    private boolean isMultipartRelated(String contentType){
        return contentType.length() >= MULTIPART.length() && 
                MULTIPART.equalsIgnoreCase(contentType.substring(0,MULTIPART.length()));
    }
    
    private boolean isFastInfoset(String contentType){
        // TODO take into account SOAP version
        return contentType.contains(FASTINFOSET);
    }
    
    private boolean isFastInfosetAcceptable(String accept) {
        // TODO take into account SOAP version
        return (accept != null) ? accept.contains(FASTINFOSET) : false;
    }
    
    /**
     * Determines the encoding codec.
     */
    private Codec getEncoder(Packet p){

        if (p.contentNegotiation == ContentNegotiation.none) {
            // The client may have changed the negotiation property from
            // pessismistic to none between invocations
            _useFastInfosetForEncoding = false;
        } else if (p.contentNegotiation == ContentNegotiation.optimistic) {
            // Always encode using Fast Infoset if in optimisitic mode
            _useFastInfosetForEncoding = true;
        }
        
        // Override the MTOM binding for now
        // Note: Using FI with MTOM does not make sense
        if (_useFastInfosetForEncoding && fiSoapCodec != null) {
            final Message m = p.getMessage();
            if(m==null || m.getAttachments().isEmpty())
                return fiSoapCodec;
            else
                // TODO: swaRef attachements
                throw new RuntimeException("TODO: Fast Infoset with swaRef attachments");
        } 
                
        if(binding.isMTOMEnabled())
            return xmlMtomCodec;
        
        Message m = p.getMessage();
        if(m==null || m.getAttachments().isEmpty())
            return xmlSoapCodec;
        else
            return xmlSwaCodec;
    }
    
    /**
     * Obtain an FI SOAP codec instance using reflection.
     */
    private static Codec getFICodec(SOAPVersion version) {
        try {
            Class c = Class.forName("com.sun.xml.ws.encoding.fastinfoset.FastInfosetStreamSOAPCodec");
            Method m = c.getMethod("create", SOAPVersion.class);
            return (Codec)m.invoke(null, version);
        } catch (Exception e) {
            return null;
        }
    }
}
