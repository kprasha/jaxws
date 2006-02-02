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
import com.sun.xml.ws.api.message.Packet;

/**
 * Base representation an XML or SOAP message as stream.
 *
 */
abstract class StreamBasedMessage {
    /**
     * The properties of the message.
     */    
    public final Packet properties;
    
    /**
     * The attachments of this message
     * (attachments live outside a message.)
     */
    public final AttachmentSet attachments;
    
    /**
     * Create a new message.
     *
     * @param properties
     *      the properties of the message.
     *
     */
    protected StreamBasedMessage(Packet properties) {
        this.properties = properties;
        this.attachments = AttachmentSet.EMPTY;        
    }
    
    /**
     * Create a new message.
     *
     * @param properties
     *      the properties of the message.
     *
     * @param attachments
     *      the attachments of the message.
     */
    protected StreamBasedMessage(Packet properties, AttachmentSet attachments) {
        this.properties = properties;
        this.attachments = attachments;
    }
}
