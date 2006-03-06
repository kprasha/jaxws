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

    public WSDLInputImpl(String name, QName messageName, WSDLOperationImpl operation) {
        this.name = name;
        this.messageName = messageName;
        this.operation = operation;
    }

    public String getName() {
        if(name != null)
            return name;
        
        return (operation.isOneWay())?operation.getName().getLocalPart():operation.getName().getLocalPart()+"Request";
    }

    public WSDLMessage getMessage() {
        return message;
    }

    void freeze(WSDLModelImpl parent) {
        message = parent.getMessage(messageName);
    }
}
