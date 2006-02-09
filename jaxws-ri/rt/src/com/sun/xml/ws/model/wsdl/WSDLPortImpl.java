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
package com.sun.xml.ws.model.wsdl;

import com.sun.xml.ws.api.EndpointAddress;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.wsdl.parser.RuntimeWSDLParser;

import javax.xml.namespace.QName;

/**
 * Implementation of {@link WSDLPort}
 *
 * @author Vivek Pandey
 */
public final class WSDLPortImpl extends AbstractExtensibleImpl implements WSDLPort {
    private final QName name;
    private EndpointAddress address;
    private final QName bindingName;
    private final WSDLServiceImpl owner;

    /**
     * To be set after the WSDL parsing is complete.
     */
    private WSDLBoundPortTypeImpl boundPortType;

    public WSDLPortImpl(WSDLServiceImpl owner, QName name, QName binding) {
        this.owner = owner;
        this.name = name;
        this.bindingName = binding;
    }

    public QName getName() {
        return name;
    }

    public QName getBindingName() {
        return bindingName;
    }

    public EndpointAddress getAddress() {
        return address;
    }

    public WSDLServiceImpl getOwner() {
        return owner;
    }

    /**
     * Only meant for {@link RuntimeWSDLParser} to call.
     */
    public void setAddress(EndpointAddress address) {
        assert address!=null;
        this.address = address;
    }

    public WSDLBoundPortTypeImpl getBinding() {
        return boundPortType;
    }

    public SOAPVersion getSOAPVersion(){
        return boundPortType.getSOAPVersion();
    }

    void freeze(WSDLModelImpl root) {
        boundPortType = root.getBinding(bindingName);
        // TODO: error check needs to be done for boundPortType==null case.
        // that's an error in WSDL which needs to be reported
    }
}