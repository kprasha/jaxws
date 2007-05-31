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
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.xml.ws.message;

import com.sun.istack.NotNull;
import com.sun.xml.bind.api.Bridge;
import com.sun.xml.messaging.saaj.util.ByteOutputStream;
import com.sun.xml.ws.api.message.Attachment;
import com.sun.xml.ws.util.ASCIIUtility;
import com.sun.xml.ws.util.ByteArrayDataSource;
import java.io.ByteArrayInputStream;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.xml.bind.JAXBException;
import javax.xml.soap.AttachmentPart;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.xml.ws.WebServiceException;

/**
 * @author Jitendra Kotamraju
 */
public final class JAXBAttachment implements Attachment, DataSource {

    private final String contentId;
    private final String mimeType;
    private final Object jaxbObject;
    private final Bridge bridge;

    public JAXBAttachment(@NotNull String contentId, Object jaxbObject, Bridge bridge, String mimeType) {
        this.contentId = contentId;
        this.jaxbObject = jaxbObject;
        this.bridge = bridge;
        this.mimeType = mimeType;
    }

    public String getContentId() {
        return contentId;
    }

    public String getContentType() {
        return mimeType;
    }

    public byte[] asByteArray() {
        try {
            return ASCIIUtility.getBytes(asInputStream());
        } catch (IOException e) {
            throw new WebServiceException(e);
        }
    }

    public DataHandler asDataHandler() {
        return new DataHandler(this);
    }

    public Source asSource() {
        return new StreamSource(asInputStream());
    }

    public InputStream asInputStream() {
        ByteOutputStream bos = new ByteOutputStream();
        try {
            writeTo(bos);
        } catch (IOException e) {
            throw new WebServiceException(e);
        }
        return bos.newInputStream();
    }

    public void writeTo(OutputStream os) throws IOException {
        try {
            bridge.marshal(jaxbObject, os, null);
        } catch (JAXBException e) {
            throw new WebServiceException(e);
        }
    }

    public void writeTo(SOAPMessage saaj) throws SOAPException {
        AttachmentPart part = saaj.createAttachmentPart();
        part.setDataHandler(asDataHandler());
        part.setContentId(contentId);
        saaj.addAttachmentPart(part);
    }

    public InputStream getInputStream() throws IOException {
        return asInputStream();
    }

    public OutputStream getOutputStream() throws IOException {
        throw new UnsupportedOperationException();
    }

    public String getName() {
        return null;
    }

}
