package com.sun.xml.ws.api.model.wsdl;

/**
 * Abstraction of wsdl:portType/wsdl:operation/wsdl:input
 *
 * @author Vivek Pandey
 */
public interface WSDLInput extends WSDLObject, WSDLExtensible {
    /**
     * Gives the wsdl:portType/wsdl:operation/wsdl:input@name
     */
    String getName();

    /**
     * Gives the WSDLMessage corresponding to wsdl:input@message
     * <p/>
     * This method should not be called before the entire WSDLModel is built. Basically after the WSDLModel is built
     * all the references are resolve in a post processing phase. IOW, the WSDL extensions should
     * not call this method.
     *
     * @return Always returns null when called from inside WSDL extensions.
     */
    WSDLMessage getMessage();
}
