package com.sun.tools.ws.api.wsdl;

/**
 * A WSDL extension
 *
 * @author Vivek Pandey
 */
public interface TExtension {
    /**
     * Gives Parent {@link TExtensible} element
     */
    TExtensible getParent();
}
