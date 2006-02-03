package com.sun.xml.ws.api.model.wsdl;

import com.sun.xml.ws.api.model.wsdl.WSDLExtensible;

import javax.xml.namespace.QName;

/**
 * Provides abstraction of wsdl:portType/wsdl:operation.
 *
 * @author Vivek Pandey
 */
public interface WSDLOperation extends WSDLObject, WSDLExtensible {
    /**
     * Gets the name of the wsdl:portType/wsdl:operation@name attribute value as local name and wsdl:definitions@targetNamespace
     * as the namespace uri.
     */
    QName getName();

    /**
     * Gets the local name portion of {@link #getName()}.
     */
    String getLocalName();

    /**
     * Gets the input message of this operation.
     */
    WSDLMessage getInputMessage();

    /**
     * Gets the output message of this operation.
     *
     * @return
     *      null if this is an one-way operation.
     */
    WSDLMessage getOutputMessage();

    /**
     * Returns true if this operation is an one-way operation.
     */
    boolean isOneWay();

    /**
     * Gets the fault message of this operation.
     */
    QName getFaultMessage();
}
