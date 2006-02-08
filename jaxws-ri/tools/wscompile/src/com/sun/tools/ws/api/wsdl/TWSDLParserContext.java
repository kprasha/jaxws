package com.sun.tools.ws.api.wsdl;

import org.w3c.dom.Element;

/**
 * Provides WSDL parsing context. It should be used by the WSDL extension handlers to register their namespaces so that
 * it can be latter used by other extensions to resolve the namespaces.
 *
 * @author Vivek Pandey
 */
public interface TWSDLParserContext {

    /**
     * Pushes the parsing context
     */
    void push();

    /**
     * pops the parsing context
     */
    void pop();

    /**
     * Gives the namespace URI for a given prefix
     *
     * @param prefix non-null prefix
     * @return null of the prefix is not found
     */
    String getNamespaceURI(String prefix);

    /**
     * Gives the prefixes in the current context
     */
    Iterable<String> getPrefixes();

    /**
     * Gives default namespace
     *
     * @return null if there is no default namespace declaration found
     */
    String getDefaultNamespaceURI();

    /**
     * Registers naemespace declarations of a given {@link Element} found in the WSDL
     *
     * @param e {@link Element} whose namespace declarations need to be registered
     */
    void registerNamespaces(Element e);
}
