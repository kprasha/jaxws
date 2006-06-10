package com.sun.xml.ws.message.jaxb;

import com.sun.xml.ws.encoding.MimeMultipartParser;
import com.sun.xml.ws.message.stream.StreamAttachment;

import javax.xml.bind.attachment.AttachmentUnmarshaller;
import javax.xml.ws.WebServiceException;
import javax.activation.DataHandler;
import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.net.URLDecoder;

/**
 * Implementation of {@link AttachmentUnmarshaller}. It will be called by {@link JAXBMessage} to unmarshall
 * swaref types.
 *
 * @author Vivek Pandey
 *
 * @see JAXBMessage
 * @see MimeMultipartParser
 */
final class AttachmentUnmarshallerImpl extends AttachmentUnmarshaller {

    private final MimeMultipartParser mimeParser;

    public AttachmentUnmarshallerImpl(MimeMultipartParser mimeParser) {
        this.mimeParser = mimeParser;
    }

    public DataHandler getAttachmentAsDataHandler(String cid) {
        try {
            StreamAttachment att = mimeParser.getAttachmentPart(decodeCid(cid));
            return att.asDataHandler();
        } catch (IOException e) {
            throw new WebServiceException(e);
        }
    }

    //This should not get called as XOP processing is disabled.
    public byte[] getAttachmentAsByteArray(String cid) {
        throw new UnsupportedOperationException();
    }

    private String decodeCid(String cid){
        if(cid.startsWith("cid:"))
            cid = cid.substring(4, cid.length());
        try {
            return "<"+ URLDecoder.decode(cid, "UTF-8")+">";
        } catch (UnsupportedEncodingException e) {
            throw new WebServiceException(e);
        }
    }
}
