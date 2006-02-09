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
package com.sun.xml.ws.client;

import com.sun.xml.ws.model.wsdl.WSDLPortImpl;
import com.sun.xml.ws.server.RuntimeContext;
import com.sun.xml.ws.api.EndpointAddress;
import com.sun.xml.ws.model.wsdl.WSDLPortImpl;
import com.sun.xml.ws.model.SOAPSEIModel;
import com.sun.xml.ws.model.AbstractSEIModelImpl;

import javax.xml.namespace.QName;


/**
 * $author: WS Development Team
 */
public class EndpointIFContext {

    private Class serviceInterface;    //prop can take out
    private QName serviceName;
    private final Class sei;
    private QName portName;
    private EndpointAddress endpointAddress;
    private String bindingId;

    private SOAPSEIModel model;


    public EndpointIFContext(Class sei) {
        this.sei = sei;
    }

    public Class getServiceInterface() {
        return serviceInterface;
    }

    public void setServiceInterface(Class serviceInterface) {
        this.serviceInterface = serviceInterface;
    }

    public Class getSei() {
        return sei;
    }

    public QName getPortName() {
        if (portName == null && model!=null){
            // TODO: I can instinctively tell that something is wrong with this!
            portName = model.getPortName();
        }
        return portName;
    }

    public EndpointAddress getEndpointAddress() {
        return endpointAddress;
    }

    public void setPortInfo(WSDLPortImpl port) {
        portName = port.getName();
        endpointAddress = port.getAddress();
        bindingId = port.getBinding().getBindingId();
    }

    public String getBindingID() {
        return bindingId;
    }

    public QName getServiceName() {
        return serviceName;
    }

    public boolean contains(QName serviceName) {
        if (serviceName.equals(this.serviceName))
            return true;
        return false;
    }

    public void setServiceName(QName serviceName) {
        this.serviceName = serviceName;
    }

    public void setBindingID(String bindingId) {
        this.bindingId = bindingId;
    }

    public SOAPSEIModel getModel() {
        return model;
    }

    public void setModel(SOAPSEIModel model) {
        this.model = model;
    }
}