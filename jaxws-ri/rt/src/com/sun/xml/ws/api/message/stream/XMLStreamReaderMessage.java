/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 *
 * You can obtain a copy of the license at
 * https://jwsdp.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * HEADER in each file and include the License file at
 * https://jwsdp.dev.java.net/CDDLv1.0.html  If applicable,
 * add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your
 * own identifying information: Portions Copyright [yyyy]
 * [name of copyright owner]
 */
package com.sun.xml.ws.api.message.stream;

import com.sun.xml.ws.api.message.AttachmentSet;
import com.sun.xml.ws.api.message.MessageProperties;
import javax.xml.stream.XMLStreamReader;

/**
 * Low level representation of an XML or SOAP message as an {@link XMLStreamReader}.
 *
 */
public class XMLStreamReaderMessage extends StreamBasedMessage {  
    /**
     * The message represented as an {@link XMLStreamReader}.
     */
    public final XMLStreamReader msg;
    
    /** 
     * Create a new message.
     *
     * @param properties
     *      the properties of the message.
     *
     * @param msg
     *      always a non-null unconsumed {@link XMLStreamReader} that
     *      represents a request.
     */
    public XMLStreamReaderMessage(MessageProperties properties, XMLStreamReader msg) {
        super(properties);
        this.msg = msg;
    }    
    
    /** 
     * Create a new message.
     *
     * @param properties
     *      the properties of the message.
     *
     * @param attachments
     *      the attachments of the message.
     *
     * @param msg
     *      always a non-null unconsumed {@link XMLStreamReader} that
     *      represents a request.
     */
    public XMLStreamReaderMessage(MessageProperties properties, AttachmentSet attachments, XMLStreamReader msg) {
        super(properties, attachments);
        this.msg = msg;
    }    
}
