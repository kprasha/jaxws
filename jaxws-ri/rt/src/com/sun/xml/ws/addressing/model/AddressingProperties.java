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

package com.sun.xml.ws.addressing.model;

import java.util.List;
import java.util.ArrayList;

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
    List<Relationship> relatesto = new ArrayList<Relationship>();
    Elements referenceParameters;
    Elements metadata;

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

    public List<Relationship> getRelatesTo() {
        return relatesto;
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

    public String getMessageid() {
        return messageid;
    }

    public void setMessageid(String messageid) {
        this.messageid = messageid;
    }

    public Elements getMetadata() {
        if (metadata == null)
            metadata = new Elements();
        return metadata;
    }

    public Elements getReferenceParameters() {
        if (referenceParameters == null)
            referenceParameters = new Elements();
        return referenceParameters;
    }

}
