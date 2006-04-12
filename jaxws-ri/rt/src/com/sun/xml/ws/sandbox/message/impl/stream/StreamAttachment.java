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

package com.sun.xml.ws.sandbox.message.impl.stream;

import com.sun.xml.ws.api.message.Attachment;
import com.sun.xml.ws.util.ByteArrayDataSource;

import javax.activation.DataHandler;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.AttachmentPart;
import javax.xml.soap.SOAPException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;

/**
 * Attachment created from raw bytes.
 *
 * @author Vivek Pandey
 */
public class StreamAttachment implements Attachment {
    private final String contentId;
    private byte[] data;
    private final String contentType;
    private int start;
    private final int len;

    public StreamAttachment(byte[] data, int offset, int length, String contentType, String contentId) {
        this.contentId = contentId;
        this.data = data;
        this.contentType = contentType;
        this.start = offset;
        this.len = length;
    }

    public String getContentId() {
        return contentId;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] asByteArray() {
        if(start !=0 || len!=data.length) {
            // if our buffer isn't exact, switch to the exact one
            byte[] exact = new byte[len];
            System.arraycopy(data,start,exact,0,len);
            start = 0;
            data = exact;
        }
        return data;
    }

    public DataHandler asDataHandler() {
        return new DataHandler(new ByteArrayDataSource(data,start,len,getContentType()));
    }

    public Source asSource() {
        return new StreamSource(new ByteArrayInputStream(data,start,len));
    }

    public InputStream asInputStream() {
        return new ByteArrayInputStream(data,start,len);
    }

    public void writeTo(OutputStream os) throws IOException {
        os.write(data,start,len);
    }

    public void writeTo(SOAPMessage saaj) throws SOAPException {
        AttachmentPart part = saaj.createAttachmentPart();
        part.setRawContentBytes(data,start,len,getContentType());
        part.setContentId(contentId);
        saaj.addAttachmentPart(part);
    }
}
