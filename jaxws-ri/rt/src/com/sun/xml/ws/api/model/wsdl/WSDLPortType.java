package com.sun.xml.ws.api.model.wsdl;

import com.sun.xml.ws.api.model.wsdl.Extensible;
import com.sun.xml.ws.api.model.wsdl.WSDLOperation;

import javax.xml.namespace.QName;

/**
 * Abstraction of wsdl:portType.
 *
 * @author Vivek Pandey
 */
public interface WSDLPortType extends WSDLObject, Extensible {
    /**
     * Gets the name of the wsdl:portType@name attribute value as local name and wsdl:definitions@targetNamespace
     * as the namespace uri.
     */
    public QName getName();

    /**
     * Gets the {@link WSDLOperation} for a given operation name
     *
     * @param operationName non-null operationName
     * @return null if a {@link WSDLOperation} is not found
     */
    public WSDLOperation get(String operationName);
}
