package com.sun.xml.ws.api.model.wsdl;

import com.sun.xml.ws.api.model.wsdl.Extensible;
import com.sun.xml.ws.api.model.wsdl.PortTypeOperation;

import javax.xml.namespace.QName;
import java.util.Map;

/**
 * Abstraction of wsdl:portType.
 *
 * @author Vivek Pandey
 */
public interface PortType extends Extensible {
    /**
     * Gets the name of the wsdl:portType@name attribute value as local name and wsdl:definitions@targetNamespace
     * as the namespace uri.
     */
    public QName getName();

    /**
     * Gets the {@link PortTypeOperation} for a given operation name
     *
     * @param operationName non-null operationName
     * @return null if a {@link PortTypeOperation} is not found
     */
    public PortTypeOperation get(String operationName);
}
