package com.sun.xml.ws.api.model.wsdl;

import javax.xml.namespace.QName;

/**
 * Abstraction of wsdl:portType/wsdl:operation/wsdl:output
 *
 * @author Vivek Pandey
 */
public interface WSDLOutput extends WSDLObject, WSDLExtensible{
    /**
     * Gives the wsdl:portType/wsdl:operation/wsdl:output@name
     */
    String getName();

    /**
     * Gives the WSDLMessage corresponding to wsdl:output@message
     */
    WSDLMessage getMessage();
}
