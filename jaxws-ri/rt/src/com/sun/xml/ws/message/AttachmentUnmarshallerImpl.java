package com.sun.xml.ws.message;

import com.sun.xml.ws.api.message.Attachment;
import com.sun.xml.ws.api.message.AttachmentSet;
import com.sun.xml.ws.encoding.MimeMultipartParser;
import com.sun.xml.ws.resources.EncodingMessages;

import javax.activation.DataHandler;
import javax.xml.bind.attachment.AttachmentUnmarshaller;
import javax.xml.ws.WebServiceException;

/**
 * Implementation of {@link AttachmentUnmarshaller} that uses
 * loads attachments from {@link AttachmentSet} directly.
 *
 * @author Vivek Pandey
 * @see MimeMultipartParser
 */
public final class AttachmentUnmarshallerImpl extends AttachmentUnmarshaller {

    private final AttachmentSet attachments;

    public AttachmentUnmarshallerImpl(AttachmentSet attachments) {
        this.attachments = attachments;
    }

    public DataHandler getAttachmentAsDataHandler(String cid) {
        Attachment a = attachments.get(cid);
        if(a==null)
            throw new WebServiceException(EncodingMessages.NO_SUCH_CONTENT_ID(cid));
        return a.asDataHandler();
    }

    //This should not get called as XOP processing is disabled.
    public byte[] getAttachmentAsByteArray(String cid) {
        throw new IllegalStateException();
    }
}
