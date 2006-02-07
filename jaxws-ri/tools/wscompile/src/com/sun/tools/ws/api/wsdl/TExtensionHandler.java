package com.sun.tools.ws.api.wsdl;

import org.w3c.dom.Element;

/**
 * JAXWS WSDL parser {@link com.sun.tools.ws.wsdl.parser.WSDLParser} will call an {@link TExtensionHandler} registered
 * with it for the WSDL extensibility elements thats not already defined in the WSDL 1.1 spec, such as SOAP or MIME.
 *
 * @author Vivek Pandey
 */
public interface TExtensionHandler {
    /**
     * Gives the namespace of an extensibility element.
     * <p/>
     * For example a soap 1.1 XXExtensionHandler would return <code>""http://schemas.xmlsoap.org/wsdl/soap/"</code>
     */
    String getNamespaceURI();

    /**
     * Callback for <code>wsdl:portType</code>
     *
     * @param context PWarser context that will be passed on by the wsdl parser
     * @param parent  The Parent element within which the extensibility element is defined
     * @param e       The extensibility elemenet
     * @return false if there was some error during the extension handling otherwise returns true. If returned false
     *         then the WSDL parser can abort if the wsdl extensibility element had <code>required</code> attribute set to true
     */
    boolean handlePortTypeExtension(TParserContext context, TExtensible parent, Element e);

    /**
     * Callback for <code>wsdl:definitions</code>
     *
     * @param context Parser context that will be passed on by the wsdl parser
     * @param parent  The Parent element within which the extensibility element is defined
     * @param e       The extensibility elemenet
     * @return false if there was some error during the extension handling otherwise returns true. If returned false
     *         then the WSDL parser can abort if the wsdl extensibility element had <code>required</code> attribute set to true
     */
    boolean handleDefinitionsExtension(TParserContext context, TExtensible parent, Element e);

    /**
     * Callback for <code>wsdl:type</code>
     *
     * @param context Parser context that will be passed on by the wsdl parser
     * @param parent  The Parent element within which the extensibility element is defined
     * @param e       The extensibility elemenet
     * @return false if there was some error during the extension handling otherwise returns true. If returned false
     *         then the WSDL parser can abort if the wsdl extensibility element had <code>required</code> attribute set to true
     */
    boolean handleTypesExtension(TParserContext context, TExtensible parent, Element e);

    /**
     * Callback for <code>wsdl:binding</code>
     *
     * @param context Parser context that will be passed on by the wsdl parser
     * @param parent  The Parent element within which the extensibility element is defined
     * @param e       The extensibility elemenet
     * @return false if there was some error during the extension handling otherwise returns true. If returned false
     *         then the WSDL parser can abort if the wsdl extensibility element had <code>required</code> attribute set to true
     */
    boolean handleBindingExtension(TParserContext context, TExtensible parent, Element e);

    /**
     * Callback for <code>wsdl:portType/wsdl:operation</code>.
     *
     * @param context Parser context that will be passed on by the wsdl parser
     * @param parent  The Parent element within which the extensibility element is defined
     * @param e       The extensibility elemenet
     * @return false if there was some error during the extension handling otherwise returns true. If returned false
     *         then the WSDL parser can abort if the wsdl extensibility element had <code>required</code> attribute set to true
     */
    boolean handleOperationExtension(TParserContext context, TExtensible parent, Element e);

    /**
     * Callback for <code>wsdl:input</code>
     *
     * @param context Parser context that will be passed on by the wsdl parser
     * @param parent  The Parent element within which the extensibility element is defined
     * @param e       The extensibility elemenet
     * @return false if there was some error during the extension handling otherwise returns true. If returned false
     *         then the WSDL parser can abort if the wsdl extensibility element had <code>required</code> attribute set to true
     */
    boolean handleInputExtension(TParserContext context, TExtensible parent, Element e);

    /**
     * Callback for <code>wsdl:output</code>
     *
     * @param context Parser context that will be passed on by the wsdl parser
     * @param parent  The Parent element within which the extensibility element is defined
     * @param e       The extensibility elemenet
     * @return false if there was some error during the extension handling otherwise returns true. If returned false
     *         then the WSDL parser can abort if the wsdl extensibility element had <code>required</code> attribute set to true
     */
    boolean handleOutputExtension(TParserContext context, TExtensible parent, Element e);

    /**
     * Callback for <code>wsdl:fault</code>
     *
     * @param context Parser context that will be passed on by the wsdl parser
     * @param parent  The Parent element within which the extensibility element is defined
     * @param e       The extensibility elemenet
     * @return false if there was some error during the extension handling otherwise returns true. If returned false
     *         then the WSDL parser can abort if the wsdl extensibility element had <code>required</code> attribute set to true
     */
    boolean handleFaultExtension(TParserContext context, TExtensible parent, Element e);

    /**
     * Callback for <code>wsdl:service</code>
     *
     * @param context Parser context that will be passed on by the wsdl parser
     * @param parent  The Parent element within which the extensibility element is defined
     * @param e       The extensibility elemenet
     * @return false if there was some error during the extension handling otherwise returns true. If returned false
     *         then the WSDL parser can abort if the wsdl extensibility element had <code>required</code> attribute set to true
     */
    boolean handleServiceExtension(TParserContext context, TExtensible parent, Element e);

    /**
     * Callback for <code>wsdl:port</code>
     *
     * @param context Parser context that will be passed on by the wsdl parser
     * @param parent  The Parent element within which the extensibility element is defined
     * @param e       The extensibility elemenet
     * @return false if there was some error during the extension handling otherwise returns true. If returned false
     *         then the WSDL parser can abort if the wsdl extensibility element had <code>required</code> attribute set to true
     */
    boolean handlePortExtension(TParserContext context, TExtensible parent, Element e);
}
