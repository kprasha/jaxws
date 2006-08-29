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

package com.sun.xml.ws.addressing.v200408;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.transform.Result;
import javax.xml.ws.EndpointReference;

import com.sun.xml.ws.addressing.model.Elements;
import static com.sun.xml.ws.addressing.v200408.MemberSubmissionAddressingConstants.WSA_ADDRESS_NAME;
import static com.sun.xml.ws.addressing.v200408.MemberSubmissionAddressingConstants.WSA_ANONYMOUS_ADDRESS;
import static com.sun.xml.ws.addressing.v200408.MemberSubmissionAddressingConstants.WSA_NAMESPACE_NAME;
import static com.sun.xml.ws.addressing.v200408.MemberSubmissionAddressingConstants.WSA_REFERENCEPARAMETERS_NAME;
import static com.sun.xml.ws.addressing.v200408.MemberSubmissionAddressingConstants.WSA_REFERENCEPROPERTIES_NAME;

/**
 * @author Arun Gupta
 */
@XmlAccessorType(value = XmlAccessType.FIELD)
@XmlRootElement(name="EndpointReference",namespace= WSA_NAMESPACE_NAME)
public class EndpointReferenceImpl extends EndpointReference {
    @XmlElement(name = WSA_ADDRESS_NAME, namespace = WSA_NAMESPACE_NAME)
    private String address;

    @XmlElement(name = WSA_REFERENCEPROPERTIES_NAME, namespace = WSA_NAMESPACE_NAME)
    private Elements refParams;

    @XmlElement(name = WSA_REFERENCEPARAMETERS_NAME, namespace = WSA_NAMESPACE_NAME)
    private Elements metadata;

    public EndpointReferenceImpl() {
        address = WSA_ANONYMOUS_ADDRESS;
    }

    public EndpointReferenceImpl(String address) {
        this.address = address;
    }

    public EndpointReferenceImpl(String address, Elements refParams) {
        this(address);
        this.refParams = refParams;
    }

    public EndpointReferenceImpl(String address, Elements refParams, Elements metadata) {
        this(address, refParams);
        this.metadata = metadata;
    }

    public void writeTo(Result result) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getAddress() {
        return address;
    }

    public Elements getMetadata() {
        return metadata;
    }

    public Elements getRefParams() {
        return refParams;
    }
}
