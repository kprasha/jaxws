package com.sun.xml.ws.api.model.wsdl;

import javax.xml.namespace.QName;

/**
 * Abstraction of wsdl:message.
 *
 * @author Vivek Pandey
 */
public interface WSDLMessage extends WSDLObject, WSDLExtensible {
    /**
     * Gives wsdl:message@name value.
     */
    QName getName();
}
