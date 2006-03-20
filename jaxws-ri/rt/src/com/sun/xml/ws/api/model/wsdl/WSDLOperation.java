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
     * Gets the wsdl:input of this operation
     */
    WSDLInput getInput();

    /**
     * Gets the wsdl:output of this operation.
     *
     * @return
     *      null if this is an one-way operation.
     */
    WSDLOutput getOutput();



    /**
     * Returns true if this operation is an one-way operation.
     */
    boolean isOneWay();

    /**
     * Gets the {link WSDLFault} corresponding to wsdl:fault of this operation.
     */
    Iterable<? extends WSDLFault> getFaults();
}
