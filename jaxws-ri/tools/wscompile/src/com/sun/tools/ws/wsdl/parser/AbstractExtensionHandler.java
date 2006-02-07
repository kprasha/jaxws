/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 * 
 * You can obtain a copy of the license at
 * https://jwsdp.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL
 * HEADER in each file and include the License file at
 * https://jwsdp.dev.java.net/CDDLv1.0.html  If applicable,
 * add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your
 * own identifying information: Portions Copyright [yyyy]
 * [name of copyright owner]
 */

package com.sun.tools.ws.wsdl.parser;

import com.sun.tools.ws.api.wsdl.TExtensible;
import com.sun.tools.ws.api.wsdl.TExtensionHandler;
import com.sun.tools.ws.api.wsdl.TParserContext;
import com.sun.tools.ws.wsdl.document.WSDLConstants;
import com.sun.tools.ws.wsdl.document.mime.MIMEConstants;
import com.sun.tools.ws.wsdl.framework.TParserContextImpl;
import org.w3c.dom.Element;

import java.util.Collections;
import java.util.Map;

/**
 * An abstract implementation class of {@link TExtensionHandler}
 *
 * @author Vivek Pandey
 */
public abstract class AbstractExtensionHandler implements TExtensionHandler {
    private final Map<String, AbstractExtensionHandler> extensionHandlers;
    private final Map<String, AbstractExtensionHandler> unmodExtenHandlers;

    public AbstractExtensionHandler(Map<String, AbstractExtensionHandler> extensionHandlerMap) {
        this.extensionHandlers = extensionHandlerMap;
        this.unmodExtenHandlers = Collections.unmodifiableMap(extensionHandlers);
    }

    public Map<String, AbstractExtensionHandler> getExtensionHandlers(){
        return unmodExtenHandlers;
    }

    /**
     * Callback that gets called by the WSDL parser or any other extension handler on finding an extensibility element
     * that it can't understand.
     *
     * @param parent  The Parent element within which the extensibility element is defined
     * @param e       The extensibility elemenet
     * @return false if there was some error during the extension handling otherwise returns true. If returned false
     *         then the WSDL parser can abort if the wsdl extensibility element had <code>required</code> attribute set to true
     */
    protected boolean doHandleExtension(TParserContext context, TExtensible parent, Element e) {
        if (parent.getWSDLElementName().equals(WSDLConstants.QNAME_DEFINITIONS)) {
            return handleDefinitionsExtension(context, parent, e);
        } else if (parent.getWSDLElementName().equals(WSDLConstants.QNAME_TYPES)) {
            return handleTypesExtension(context, parent, e);
        } else if (parent.getWSDLElementName().equals(WSDLConstants.QNAME_PORT_TYPE)) {
            return handlePortTypeExtension(context, parent, e);
        } else if (
            parent.getWSDLElementName().equals(WSDLConstants.QNAME_BINDING)) {
            return handleBindingExtension(context, parent, e);
        } else if (
            parent.getWSDLElementName().equals(WSDLConstants.QNAME_OPERATION)) {
            return handleOperationExtension(context, parent, e);
        } else if (parent.getWSDLElementName().equals(WSDLConstants.QNAME_INPUT)) {
            return handleInputExtension(context, parent, e);
        } else if (
            parent.getWSDLElementName().equals(WSDLConstants.QNAME_OUTPUT)) {
            return handleOutputExtension(context, parent, e);
        } else if (parent.getWSDLElementName().equals(WSDLConstants.QNAME_FAULT)) {
            return handleFaultExtension(context, parent, e);
        } else if (
            parent.getWSDLElementName().equals(WSDLConstants.QNAME_SERVICE)) {
            return handleServiceExtension(context, parent, e);
        } else if (parent.getWSDLElementName().equals(WSDLConstants.QNAME_PORT)) {
            return handlePortExtension(context, parent, e);
        } else if (parent.getWSDLElementName().equals(MIMEConstants.QNAME_PART)) {
            return handleMIMEPartExtension(context, parent, e);
        } else {
            return false;
        }
    }

    /**
     * Callback for <code>wsdl:mime</code>
     *
     * @param context Parser context that will be passed on by the wsdl parser
     * @param parent  The Parent element within which the extensibility element is defined
     * @param e       The extensibility elemenet
     * @return false if there was some error during the extension handling otherwise returns true. If returned false
     *         then the WSDL parser can abort if the wsdl extensibility element had <code>required</code> attribute set to true
     */
    protected boolean handleMIMEPartExtension(TParserContext context, TExtensible parent, Element e){
        return false;
    }

    public boolean handlePortTypeExtension(TParserContext context, TExtensible parent, Element e) {
        return false;
    }

    public boolean handleDefinitionsExtension(TParserContext context, TExtensible parent, Element e) {
        return false;
    }

    public boolean handleTypesExtension(TParserContext context, TExtensible parent, Element e) {
        return false;
    }

    public boolean handleBindingExtension(TParserContext context, TExtensible parent, Element e) {
        return false;
    }

    public boolean handleOperationExtension(TParserContext context, TExtensible parent, Element e) {
        return false;
    }

    public boolean handleInputExtension(TParserContext context, TExtensible parent, Element e) {
        return false;
    }

    public boolean handleOutputExtension(TParserContext context, TExtensible parent, Element e) {
        return false;
    }

    public boolean handleFaultExtension(TParserContext context, TExtensible parent, Element e) {
        return false;
    }

    public boolean handleServiceExtension(TParserContext context, TExtensible parent, Element e) {
        return false;
    }

    public boolean handlePortExtension(TParserContext context, TExtensible parent, Element e) {
        return false;
    }
}
