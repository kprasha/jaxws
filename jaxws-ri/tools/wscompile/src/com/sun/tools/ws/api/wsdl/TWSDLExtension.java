package com.sun.tools.ws.api.wsdl;

/**
 * A WSDL extension
 *
 * @author Vivek Pandey
 */
public interface TWSDLExtension {
    /**
     * Gives Parent {@link TWSDLExtensible} element
     */
    TWSDLExtensible getParent();
}
