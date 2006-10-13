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

package com.sun.xml.ws.api.addressing;

import javax.xml.ws.WebServiceFeature;

/**
 * This feature allows ReplyTo and FaultTo Message Addressing Properties
 * to be added for a one-way message. This feature should be used for one-way
 * operations only.
 * <p/><p/>
 * This feature is not meant to be used by a common Web service developer as there
 * is no need to send ReplyTo and/or FaultTo header for a one-way operation. But these
 * properties may need to be sent in certain middleware Web services.
 *
 * @author Arun Gupta
 */
public class OneWayFeature extends WebServiceFeature {
    /**
     * Constant value identifying the {@link OneWayFeature}
     */
    public static final String ID = "http://java.sun.com/xml/ns/jaxws/addressing/oneway";

    private String replyToAddress;
    private String faultToAddress;

    /**
     * Create an {@link OneWayFeature}. The instance created will be enabled.
     */
    public OneWayFeature() {
        this.enabled = true;
    }

    /**
     * Create an {@link OneWayFeature}
     *
     * @param enabled specifies whether this feature should
     *                be enabled or not.
     */
    public OneWayFeature(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Create an {@link OneWayFeature}
     *
     * @param enabled specifies whether this feature should
     * be enabled or not.
     * @param replyTo specifies the address of wsa:ReplyTo header.
     */
    public OneWayFeature(boolean enabled, String replyTo) {
        this.enabled = enabled;
        this.replyToAddress = replyTo;
    }

    /**
     * Create an {@link OneWayFeature}
     *
     * @param enabled specifies whether this feature should
     * be enabled or not.
     * @param replyTo specifies the address of wsa:ReplyTo header.
     * @param faultTo specifies the address of wsa:FaultTo header.
     */
    public OneWayFeature(boolean enabled, String replyTo, String faultTo) {
        this.enabled = enabled;
        this.replyToAddress = replyTo;
        this.faultToAddress = faultTo;
    }

    /**
     * {@inheritDoc}
     */
    public String getID() {
        return ID;
    }

    /**
     * Getter for wsa:ReplyTo header address
     *
     * @return address of the wsa:ReplyTo header
     */
    public String getReplyToAddress() {
        return replyToAddress;
    }

    /**
     * Setter for wsa:ReplyTo header address.
     *
     * @param address
     */
    public void setReplyToAddress(String address) {
        this.replyToAddress = address;
    }

    /**
     * Getter for wsa:FaultTo header address
     *
     * @return address of the wsa:FaultTo header
     */
    public String getFaultToAddress() {
        return faultToAddress;
    }

    /**
     * Setter for wsa:FaultTo header address.
     *
     * @param address
     */
    public void setFaultToAddress(String address) {
        this.faultToAddress = address;
    }
}
