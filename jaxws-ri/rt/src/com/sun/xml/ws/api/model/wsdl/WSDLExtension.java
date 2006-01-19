package com.sun.xml.ws.api.model.wsdl;

import javax.xml.namespace.QName;

/**
 * Represents a WSDL extensibility element or attribute.
 *
 * @author Vivek Pandey
 */
public interface WSDLExtension {
    /**
     * Gets the qualified name of the WSDL extensibility element or attribute.
     */
    public QName getName();
}
