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

import com.sun.xml.stream.buffer.XMLStreamBufferException;
import com.sun.xml.ws.addressing.W3CAddressingConstants;
import com.sun.xml.ws.addressing.v200408.MemberSubmissionAddressingConstants;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.ws.soap.AddressingFeature;

/**
 * 'Traits' object that absorbs differences of WS-Addressing versions.
 *
 * @author Arun Gupta
 */
public enum AddressingVersion {
    W3C(W3CAddressingConstants.WSA_NAMESPACE_NAME,"w3c-anonymous-epr.xml"),
    MEMBER(MemberSubmissionAddressingConstants.WSA_NAMESPACE_NAME,"member-anonymous-epr.xml");

    public final String nsUri;

    /**
     * Represents the anonymous EPR.
     */
    public final WSEndpointReference anonymousEpr;
    /**
     * Represents the ReplyTo for a specific WS-Addressing Version.
     * For example, wsa:ReplyTo where wsa binds to "http://www.w3.org/2005/08/addressing"
     */
    public final QName toTag;
    public final QName fromTag;
    public final QName replyToTag;
    public final QName faultToTag;
    public final QName actionTag;
    public final QName messageIDTag;


    private AddressingVersion(String nsUri, String anonymousEprResrouceName) {
        this.nsUri = nsUri;
        toTag = new QName(nsUri,"To");
        fromTag = new QName(nsUri,"From");
        replyToTag = new QName(nsUri,"ReplyTo");
        faultToTag = new QName(nsUri,"FaultTo");
        actionTag = new QName(nsUri,"Action");
        messageIDTag = new QName(nsUri,"MessageID");

        // create stock anonymous EPR
        try {
            this.anonymousEpr = new WSEndpointReference(getClass().getResourceAsStream(anonymousEprResrouceName));
        } catch (XMLStreamException e) {
            throw new Error(e); // bug in our code as EPR should parse.
        } catch (XMLStreamBufferException e) {
            throw new Error(e); // bug in our code
        }
    }

    /**
     * Returns {@link AddressingVersion} whose {@link #nsUri} equals to
     * the given string.
     *
     * This method does not perform input string validation.
     *
     * @param nsUri
     *      must not be null.
     * @return always non-null.
     */
    public static AddressingVersion fromNsUri(String nsUri) {
        if (nsUri.equals(W3C.nsUri))
            return W3C;

        if (nsUri.equals(MEMBER.nsUri))
            return MEMBER;

        return null;
    }

    public static AddressingVersion fromBinding(WSBinding binding) {
        if (binding.hasFeature(AddressingFeature.ID))
            return W3C;

        if (binding.hasFeature(MemberSubmissionAddressingFeature.ID))
            return MEMBER;

        return null;
    }

    public static AddressingVersion fromPort(WSDLPort port) {
        String ns = port.getBinding().getAddressingVersion();
        if (ns.equals(W3C.nsUri))
            return W3C;

        if (ns.equals(MEMBER.nsUri))
            return MEMBER;

        return null;
    }

    /**
     * Returns {@link #nsUri} associated with this {@link AddressingVersion}
     *
     * @return namespace URI
     */
    public String getNsUri() {
        return nsUri;
    }
}
