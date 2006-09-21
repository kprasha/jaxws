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

import javax.xml.ws.soap.AddressingFeature;

import com.sun.xml.ws.addressing.W3CAddressingConstants;
import com.sun.xml.ws.addressing.v200408.MemberSubmissionAddressingConstants;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;

/**
 * @author Arun Gupta
 */
public enum AddressingVersion {
    W3C(W3CAddressingConstants.WSA_NAMESPACE_NAME),
    MEMBER(MemberSubmissionAddressingConstants.WSA_NAMESPACE_NAME);

    public final String nsUri;

    private AddressingVersion(String nsUri) {
        this.nsUri = nsUri;
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

    public boolean equals(AddressingVersion that) {
        if (that == null)
            return false;

        return nsUri.equals(that.nsUri);
    }
}
