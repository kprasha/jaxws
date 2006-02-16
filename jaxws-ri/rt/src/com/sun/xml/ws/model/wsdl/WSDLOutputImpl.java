package com.sun.xml.ws.model.wsdl;

import com.sun.xml.ws.api.model.wsdl.WSDLOutput;
import com.sun.xml.ws.api.model.wsdl.WSDLMessage;

import javax.xml.namespace.QName;

/**
 * @author Vivek Pandey
 */
public class WSDLOutputImpl extends AbstractExtensibleImpl implements WSDLOutput {
    private String name;
    private QName messageName;
    private WSDLOperationImpl operation;
    private WSDLMessageImpl message;

    public WSDLOutputImpl(String name, QName messageName, WSDLOperationImpl operation) {
        this.name = name;
        this.messageName = messageName;
        this.operation = operation;
    }

    public String getName() {
        return (name == null)?operation.getName().getLocalPart()+"Response":name;
    }

    public WSDLMessage getMessage() {
        return message;
    }

    void freeze(WSDLModelImpl root) {
        message = root.getMessage(messageName);
    }
}
