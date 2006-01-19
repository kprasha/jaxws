package com.sun.xml.ws.api.model.wsdl;

import com.sun.xml.ws.api.model.wsdl.Extensible;

import javax.xml.namespace.QName;

/**
 * Provides abstraction of wsdl:portType/wsdl:operation.
 *
 * @author Vivek Pandey
 */
public interface PortTypeOperation extends Extensible {
    /**
     * Gets the name of the wsdl:portType/wsdl:operation@name attribute value as local name and wsdl:definitions@targetNamespace
     * as the namespace uri.
     */
    QName getName();

    /**
     * Gets {@link QName} the wsdl:portType/wsdl:input@message value.
     */
    QName getInputMessage();

    /**
     * Gets {@link QName} the wsdl:portType/wsdl:output@message value.
     */
    QName getOutputMessage();

    /**
     * Gets {@link QName} the wsdl:portType/wsdl:fault@message value.
     */
    QName getFaultMessage();
}
