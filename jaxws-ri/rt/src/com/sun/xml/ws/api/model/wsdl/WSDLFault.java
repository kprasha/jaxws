package com.sun.xml.ws.api.model.wsdl;

/**
 * Abstracts wsdl:portType/wsdl:operation/wsdl:fault
 *
 * @author Vivek Pandey
 */
public interface WSDLFault extends WSDLObject, WSDLExtensible{
    /**
     * Gives wsdl:fault@name value
     */
    String getName();

    /**
     * Gives the WSDLMessage corresponding to wsdl:fault@message
     */
    WSDLMessage getMessage();
}
