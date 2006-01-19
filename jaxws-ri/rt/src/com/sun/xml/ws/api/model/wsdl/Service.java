package com.sun.xml.ws.api.model.wsdl;

import com.sun.xml.ws.api.model.wsdl.Extensible;

import javax.xml.namespace.QName;
import java.util.Map;

/**
 * Abstracts wsdl:service.
 *
 * @author Vivek Pandey
 */
public interface Service extends Map<QName, Port>, Extensible {
    /**
     * Gets the name of the wsdl:service@name attribute value as local name and wsdl:definitions@targetNamespace
     * as the namespace uri.
     */
    QName getName();
}
