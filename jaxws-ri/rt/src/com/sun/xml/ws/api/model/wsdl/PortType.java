package com.sun.xml.ws.api.model.wsdl;

import com.sun.xml.ws.api.model.wsdl.Extensible;
import com.sun.xml.ws.api.model.wsdl.PortTypeOperation;

import javax.xml.namespace.QName;
import java.util.Map;

/**
 * Abstraction of wsdl:portType
 *
 * @author Vivek Pandey
 */
public interface PortType extends Map<String, PortTypeOperation>, Extensible {
    /**
     * Gets the name of the wsdl:portType@name attribute value as local name and wsdl:definitions@targetNamespace
     * as the namespace uri.
     */
    public QName getName();
}
