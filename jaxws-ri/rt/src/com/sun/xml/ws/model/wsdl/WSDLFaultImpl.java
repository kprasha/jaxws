package com.sun.xml.ws.model.wsdl;

import com.sun.xml.ws.api.model.wsdl.WSDLFault;
import com.sun.xml.ws.api.model.wsdl.WSDLMessage;

import javax.xml.namespace.QName;

/**
 * @author Vivek Pandey
 */
public class WSDLFaultImpl extends AbstractExtensibleImpl implements WSDLFault {
    private final String name;
    private final QName messageName;
    private WSDLMessageImpl message;

    public WSDLFaultImpl(String name, QName messageName) {
        this.name = name;
        this.messageName = messageName;
    }

    public String getName() {
        return name;
    }

    public WSDLMessage getMessage() {
        return message;
    }

    void freeze(WSDLModelImpl root){
        message = root.getMessage(messageName);
    }
}
