package com.sun.xml.ws.api.model.wsdl;

/**
 * Abstracts wsdl:portType/wsdl:operation/wsdl:fault
 *
 * @author Vivek Pandey
 */
public interface WSDLFault extends WSDLObject, WSDLExtensible {
    /**
     * Gives wsdl:fault@name value
     */
    String getName();

    /**
     * Gives the WSDLMessage corresponding to wsdl:fault@message
     * This method should not be called before the entire WSDLModel is built. Basically after the WSDLModel is built
     * all the references are resolve in a post processing phase. IOW, the WSDL extensions should
     * not call this method.
     *
     * @return Always returns null when called from inside WSDL extensions.
     */
    WSDLMessage getMessage();
}
