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
 * This feature allows a wsa:ReplyTo header to be specified for one-way
 * operations. This feature is not meant to be used by a common
 * Web service developer as there is no need to send ReplyTo header for
 * a one-way operation. But this header may need to be sent in certain
 * middleware Web services.
 *
 * @author Arun Gupta
 */
public class OneWayReplyToFeature extends WebServiceFeature {
    /**
     * Constant value identifying the {@link OneWayReplyToFeature}
     */
    public static final String ID = "http://java.sun.com/xml/ns/jaxws/addressing/onewayreplyto";

    private String address;

    /**
     * Create an {@link OneWayReplyToFeature}. The instance created will be enabled.
     */
    public OneWayReplyToFeature() {
        this.enabled = true;
    }

    /**
     * Create an {@link OneWayReplyToFeature}
     *
     * @param enabled specifies whether this feature should
     *                be enabled or not.
     */
    public OneWayReplyToFeature(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Create an {@link OneWayReplyToFeature}
     *
     * @param enabled specifies whether this feature should
     * be enabled or not.
     * @param address specifies the address of wsa:ReplyTo header.
     */
    public OneWayReplyToFeature(boolean enabled, String address) {
        this.enabled = enabled;
        this.address = address;
    }

    /**
     * {@inheritDoc}
     */
    public String getID() {
        return ID;
    }

    /**
     * Getter for address
     *
     * @return address of the wsa:ReplyTo header
     */
    public String getAddress() {
        return address;
    }

    /**
     * Setter for address.
     *
     * @param address
     */
    public void setAddress(String address) {
        this.address = address;
    }
}
