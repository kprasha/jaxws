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


import com.sun.xml.messaging.saaj.packaging.mime.MessagingException;
import com.sun.xml.messaging.saaj.packaging.mime.internet.ContentType;
import com.sun.xml.messaging.saaj.packaging.mime.internet.InternetHeaders;
import com.sun.xml.messaging.saaj.packaging.mime.internet.ParseException;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.Decoder;
import com.sun.xml.ws.message.stream.StreamAttachment;
import com.sun.xml.ws.util.ASCIIUtility;
import com.sun.xml.ws.encoding.ByteOutputStream;

import javax.xml.ws.WebServiceException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Decoder that serves as Multipart/Related messages decoder. Internally it delegates decoding to
 * {@link StreamSOAPDecoder} for normal MR messages with root part as SOAP 1.1 or SOAP 1.2 envelope. If the MR
 * message is XOP packaged then it delegates decoding to {@link MtomDecoder}.
 *
 * @author Vivek Pandey
 */

public class MimeMultipartRelatedDecoder implements Decoder {

    private MimeMultipartParser parser;
    private final Decoder mtomDecoder;
    private final Decoder soapDecoder;

    /**
     * Tells if the root part is mtom encoded
     */
    private boolean mtomEncoded;

    private final SOAPVersion version;
    private SOAPVersion parsedVersion;

    public MimeMultipartRelatedDecoder(SOAPVersion version) {
        this.version = version;
        mtomDecoder = new MtomDecoder(this, version);
        soapDecoder = StreamSOAPDecoder.create(version);
    }

    public void decode(InputStream in, String contentType, Packet packet) throws IOException {
        try {
            /**
             * A xop packaged Content-Type header would tell whether its MTOM message or not, it also tells the root
             * parts 'type' paramenter. Lets not check for it now as the root part will give all this information thru
             * its Content-Type.
             *
             * This is how a XOP packaged Contnet-Type header looks
             *
             * multipart/related; type="application/xop+xml";start="<http://tempuri.org/0>";boundary="uuid:0ca0e16e-feb1-426c-97d8-c4508ada5e82+id=1";start-info="text/xml"
             */
            ContentType ct = new ContentType(contentType);
            //This decoder cant handle the content-type other than Multipart/Related
            //TODO throw some exception that can be caught by the caller to invoke appropriate decoder
            if (!ct.getPrimaryType().equalsIgnoreCase("Multipart") || !ct.getSubType().equalsIgnoreCase("Related"))
                throw new WebServiceException("Incorrect Content-Type: " + ct.getPrimaryType() + "/" + ct.getSubType());

        } catch (ParseException e) {
            //TODO: dont we need DecoderExcpetion or somthing of that sort
            throw new WebServiceException(e);
        }
        
        parser = new MimeMultipartParser(in, contentType);
        StreamAttachment root = parser.getRootPart();
        try {
            String ctype = root.getContentType();
            if (ctype != null) {
                SOAPVersion parsedVersion = parseContentType(root.getContentType());
                if(parsedVersion.compareTo(version) != 0){
                    //TODO: i18nify
                    throw new WebServiceException("Incorrect Content-Type, expecting: " + version.toString()+", got: "+parsedVersion);
                }
                InputStream bodyStream = root.asInputStream();
                if (mtomEncoded) {
                    mtomDecoder.decode(bodyStream, ctype, packet);
                    return;
                } else {
                    soapDecoder.decode(bodyStream, ctype, packet);
                    return;
                }
            }
        } catch (MessagingException e) {
            throw new WebServiceException(e);
        }

        // BUG: not allowed to leave a message without a packet - KK
        throw new UnsupportedOperationException();
    }
    
    public StreamAttachment getMIMEPart(String contentId) throws IOException {
        return parser.getAttachmentPart(contentId);
    }

    public void decode(ReadableByteChannel in, String contentType, Packet packet) {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates a copy of this {@link com.sun.xml.ws.api.pipe.Decoder}.
     * <p/>
     * <p/>
     * See {@link com.sun.xml.ws.api.pipe.Encoder#copy()} for the detailed contract.
     */
    public Decoder copy() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    private SOAPVersion parseContentType(String contentType) throws ParseException {
        SOAPVersion parsedVersion=null;
        ContentType ct = new ContentType(contentType);
        String base = ct.getPrimaryType();
        String sub = ct.getSubType();
        if (base.equals("text") && sub.equals("xml")) {
            parsedVersion = SOAPVersion.SOAP_11;
        } else if (base.equals("application") && sub.equals("soap+xml")) {
            parsedVersion = SOAPVersion.SOAP_11;
        } else if (base.equals("application") && sub.equals("xop+xml")) {
            mtomEncoded = true;
            String type = ct.getParameter("type");
            if (type.equals("text/xml"))
                parsedVersion = SOAPVersion.SOAP_11;
            else if (type.equals("application/soap+xml"))
                parsedVersion = SOAPVersion.SOAP_12;
            //TODO: XML/HTTP
        }
        return parsedVersion;
    }

    public static final Decoder SOAP11 = new MimeMultipartRelatedDecoder(SOAPVersion.SOAP_11);
    public static final Decoder SOAP12 = new MimeMultipartRelatedDecoder(SOAPVersion.SOAP_12);

    public static Decoder get(SOAPVersion version) {
        if(version==null)
            // this decoder is for SOAP, not for XML/HTTP
            throw new IllegalArgumentException();
        switch(version) {
        case SOAP_11:
            return SOAP11;
        case SOAP_12:
            return SOAP12;
        default:
            throw new AssertionError();
        }
    }
}
