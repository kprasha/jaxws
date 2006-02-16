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

import com.sun.xml.ws.api.model.wsdl.WSDLOperation;
import com.sun.xml.ws.api.model.wsdl.WSDLMessage;
import com.sun.xml.ws.api.model.wsdl.WSDLOutput;
import com.sun.xml.ws.api.model.wsdl.WSDLFault;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.ArrayList;

/**
 * Implementaiton of {@link WSDLOperation}
 *
 * @author Vivek Pandey
 */
public final class WSDLOperationImpl extends AbstractExtensibleImpl implements WSDLOperation {
    private final QName name;
    private String parameterOrder;
    private WSDLInputImpl input;
    private WSDLOutputImpl output;
    private final List<WSDLFaultImpl> faults;
    protected Iterable<WSDLMessageImpl> messages;

    public WSDLOperationImpl(QName name) {
        this.name = name;
        this.faults = new ArrayList<WSDLFaultImpl>();
    }

    public QName getName() {
        return name;
    }

    public String getLocalName() {
        return name.getLocalPart();
    }

    public String getParameterOrder() {
        return parameterOrder;
    }

    public void setParameterOrder(String parameterOrder) {
        this.parameterOrder = parameterOrder;
    }

    public WSDLInputImpl getInput() {
        return input;
    }

    public void setInput(WSDLInputImpl input) {
        this.input = input;
    }

    public WSDLOutput getOutput() {
        return output;
    }

    public boolean isOneWay() {
        return output == null;
    }

    public void setOutput(WSDLOutputImpl output) {
        this.output = output;
    }

    public Iterable<WSDLFaultImpl> getFaults() {
        return faults;
    }

    public void addFault(WSDLFaultImpl fault) {
        faults.add(fault);
    }

    public void freez(WSDLModelImpl root) {
        assert input != null;
        input.freeze(root, this);
        if(output != null)
            output.freeze(root, this);
        for(WSDLFaultImpl fault : faults){
            fault.freeze(root);
        }
    }
}
