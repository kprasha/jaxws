package com.sun.xml.ws.model.wsdl;

import com.sun.xml.ws.api.model.wsdl.WSDLPartDescriptor;
import com.sun.xml.ws.api.model.wsdl.WSDLDescriptorKind;

import javax.xml.namespace.QName;

/**
 * @author Vivek Pandey
 */
public class WSDLPartDescriptorImpl implements WSDLPartDescriptor {
    private QName name;
    private WSDLDescriptorKind type;

    public WSDLPartDescriptorImpl(QName name, WSDLDescriptorKind kind) {
        this.name = name;
        this.type = kind;
    }

    public QName name() {
        return name;
    }

    public WSDLDescriptorKind type() {
        return type;
    }
}
