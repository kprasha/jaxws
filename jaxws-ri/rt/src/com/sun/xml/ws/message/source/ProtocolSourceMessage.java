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

package com.sun.xml.ws.message.source;

import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.message.AttachmentSet;
import com.sun.xml.ws.api.message.HeaderList;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Messages;
import com.sun.xml.ws.message.AbstractMessageImpl;
import com.sun.xml.ws.streaming.XMLStreamReaderFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

/**
 * Partial implementation of {@link Message} backed by {@link Source} where the Source represents the complete message
 * such as a SOAP envelope.
 *
 * @author Vivek Pandey
 */
public class ProtocolSourceMessage extends AbstractMessageImpl {
    private HeaderList headers;
    private AttachmentSet attSet;
    private Source src;
    private Message sm;

    public ProtocolSourceMessage(Source source) {
        super((SOAPVersion)null);
        this.src = source;
        SourceUtils sourceUtils = new SourceUtils(src);
        if (sourceUtils.isStreamSource()) {
            StreamSource streamSource = (StreamSource) src;
            XMLStreamReader reader = XMLStreamReaderFactory.createXMLStreamReader(streamSource.getInputStream(), true);
            sm = Messages.create(reader);
        }
    }

    public boolean hasHeaders() {
        return (headers != null) && headers.size() > 0;
    }

    public HeaderList getHeaders() {
        if (headers == null)
            headers = new HeaderList();
        return headers;
    }

    public String getPayloadLocalPart() {
        throw new UnsupportedOperationException();
    }

    public String getPayloadNamespaceURI() {
        throw new UnsupportedOperationException();
    }

    public boolean hasPayload() {
        throw new UnsupportedOperationException();
    }

    public Source readPayloadAsSource() {
        throw new UnsupportedOperationException();
    }

    public XMLStreamReader readPayload() throws XMLStreamException {
        throw new UnsupportedOperationException();
    }

    public void writePayloadTo(XMLStreamWriter sw) throws XMLStreamException {
        throw new UnsupportedOperationException();
    }

    public void writeTo(XMLStreamWriter sw) throws XMLStreamException {
        throw new UnsupportedOperationException();
    }

    public void writePayloadTo(ContentHandler contentHandler, ErrorHandler errorHandler, boolean fragment) throws SAXException {
        throw new UnsupportedOperationException();
    }

    public Message copy() {
        throw new UnsupportedOperationException();
    }
}
