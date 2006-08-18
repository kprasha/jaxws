/*
 The contents of this file are subject to the terms
 of the Common Development and Distribution License
 (the "License").  You may not use this file except
 in compliance with the License.
 
 You can obtain a copy of the license at
 https://jwsdp.dev.java.net/CDDLv1.0.html
 See the License for the specific language governing
 permissions and limitations under the License.
 
 When distributing Covered Code, include this CDDL
 HEADER in each file and include the License file at
 https://jwsdp.dev.java.net/CDDLv1.0.html  If applicable,
 add the following below this CDDL HEADER, with the
 fields enclosed by brackets "[]" replaced with your
 own identifying information: Portions Copyright [yyyy]
 [name of copyright owner]
*/
/*
 $Id: AddressingProperties.java,v 1.1.2.1 2006-08-18 21:56:11 arungupta Exp $

 Copyright (c) 2006 Sun Microsystems, Inc.
 All rights reserved.
*/

package com.sun.xml.ws.addressing;

import javax.xml.ws.EndpointReference;

/**
 * @author Arun Gupta
 */
public class AddressingProperties {
    private String to;
    EndpointReference from;
    EndpointReference replyTo;
    EndpointReference faultTo;
    String action;
    String messageid;
    String relationship;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public EndpointReference getFaultTo() {
        return faultTo;
    }

    public void setFaultTo(EndpointReference faultTo) {
        this.faultTo = faultTo;
    }

    public EndpointReference getFrom() {
        return from;
    }

    public void setFrom(EndpointReference from) {
        this.from = from;
    }

    public String getMessageID() {
        return messageid;
    }

    public void setMessageID(String messageid) {
        this.messageid = messageid;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public EndpointReference getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(EndpointReference replyTo) {
        this.replyTo = replyTo;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }
}
