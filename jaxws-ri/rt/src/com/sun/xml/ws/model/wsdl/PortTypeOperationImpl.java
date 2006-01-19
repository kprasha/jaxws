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

import com.sun.xml.ws.api.model.wsdl.PortTypeOperation;
import com.sun.xml.ws.api.model.wsdl.WSDLExtension;

import javax.xml.namespace.QName;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;

public class PortTypeOperationImpl extends AbstractExtensibleImpl implements PortTypeOperation {
    private QName name;
    private String parameterOrder;
    private QName inputMessage;
    private QName outputMessage;
    private QName faultMessage;

    public PortTypeOperationImpl(QName name) {
        this.name = name;
        extensions = new HashSet<WSDLExtension>();
    }

    public QName getName() {
        return name;
    }

    public String getParameterOrder() {
        return parameterOrder;
    }

    public void setParameterOrder(String parameterOrder) {
        this.parameterOrder = parameterOrder;
    }

    public QName getInputMessage() {
        return inputMessage;
    }

    public void setInputMessage(QName inputMessage) {
        this.inputMessage = inputMessage;
    }

    public QName getOutputMessage() {
        return outputMessage;
    }

    public void setOutputMessage(QName outputMessage) {
        this.outputMessage = outputMessage;
    }

    public QName getFaultMessage() {
        return faultMessage;
    }

    public void setFaultMessage(QName faultMessage) {
        this.faultMessage = faultMessage;
    }
}
