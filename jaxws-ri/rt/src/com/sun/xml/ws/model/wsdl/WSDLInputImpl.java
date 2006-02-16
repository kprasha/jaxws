package com.sun.xml.ws.model.wsdl;

import com.sun.xml.ws.api.model.wsdl.WSDLInput;
import com.sun.xml.ws.api.model.wsdl.WSDLMessage;

import javax.xml.namespace.QName;

/**
 * @author Vivek Pandey
 */
public class WSDLInputImpl extends AbstractExtensibleImpl implements WSDLInput {
    private String name;
    private QName messageName;
    private WSDLOperationImpl operation;
    private WSDLMessageImpl message;

    public WSDLInputImpl(String name, QName messageName) {
        this.name = name;
        this.messageName = messageName;
    }

    public String getName() {
        if(name == null){
            return operation.isOneWay()?operation.getName().getLocalPart():operation.getName().getLocalPart()+"Request";
        }
        return name;
    }

    public WSDLMessage getMessage() {
        return message;
    }

    void freeze(WSDLModelImpl parent, WSDLOperationImpl operation) {
        message = parent.getMessage(messageName);
        this.operation = operation;
    }
}
