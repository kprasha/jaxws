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
 $Id: EndpointReferenceImpl.java,v 1.1.2.1 2006-08-18 21:56:12 arungupta Exp $

 Copyright (c) 2006 Sun Microsystems, Inc.
 All rights reserved.
*/

package com.sun.xml.ws.addressing;

import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;
import javax.xml.ws.EndpointReference;
import javax.xml.transform.Result;

import org.w3c.dom.Element;

/**
 * @author Arun Gupta
 */
@XmlAccessorType(value = XmlAccessType.FIELD)
@XmlRootElement(name="EndpointReference",namespace= W3CAddressingConstants.WSA_NAMESPACE_NAME)
public class EndpointReferenceImpl extends EndpointReference {
    @XmlElement(name = "Address", namespace = W3CAddressingConstants.WSA_NAMESPACE_NAME)
    private String address;

    @XmlElement(name = "ReferenceParameters", namespace = W3CAddressingConstants.WSA_NAMESPACE_NAME)
    private Elements refParams;

    @XmlElement(name = "Metadata", namespace = W3CAddressingConstants.WSA_NAMESPACE_NAME)
    private Elements metadata;

    public EndpointReferenceImpl() {
        address = W3CAddressingConstants.WSA_ANONYMOUS_ADDRESS;
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

    public static class Elements {
        @XmlAnyElement
        List<Element> elements;
        @XmlAnyAttribute
        Map<QName,String> attributes;
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
