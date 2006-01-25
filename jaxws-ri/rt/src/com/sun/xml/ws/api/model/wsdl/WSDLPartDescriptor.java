package com.sun.xml.ws.api.model.wsdl;

import javax.xml.namespace.QName;

/**
 * Abstracts wsdl:part descriptor that is defined using element or type attribute.
 *
 * @author Vivek Pandey
 */
public interface WSDLPartDescriptor {
    /**
     * Gives Qualified name of the XML Schema element or type
     */
    public QName name();

    /**
     * Gives whether wsdl:part references a schema type or a global element.
     */
    public WSDLDescriptorKind type();

}
