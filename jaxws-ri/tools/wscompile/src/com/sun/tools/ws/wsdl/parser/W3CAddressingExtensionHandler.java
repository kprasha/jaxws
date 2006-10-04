/*
 The contents of this file are subject to the terms
 of the Common Development and Distribution License
 (the "License").  You may not use this file except
 in compliance with the License.
 
 You can obtain a copy of the license at
 https://jwsdp.dev.java.net/CDDLv1.0.html
 See the License for the specific language governing
 permissions and limitations under the License.
 
 When distributing Covered Code, include this CDDL
 HEADER in each file and include the License file at
 https://jwsdp.dev.java.net/CDDLv1.0.html  If applicable,
 add the following below this CDDL HEADER, with the
 fields enclosed by brackets "[]" replaced with your
 own identifying information: Portions Copyright [yyyy]
 [name of copyright owner]
*/
/*
 $Id: W3CAddressingExtensionHandler.java,v 1.1.2.6 2006-10-04 19:01:20 arungupta Exp $

 Copyright (c) 2006 Sun Microsystems, Inc.
 All rights reserved.
*/

package com.sun.tools.ws.wsdl.parser;

import java.util.Map;

import javax.xml.namespace.QName;

import com.sun.tools.ws.api.wsdl.TWSDLExtensible;
import com.sun.tools.ws.api.wsdl.TWSDLParserContext;
import com.sun.tools.ws.processor.util.ProcessorEnvironment;
import com.sun.tools.ws.resources.WsdlMessages;
import com.sun.tools.ws.util.xml.XmlUtil;
import com.sun.tools.ws.wsdl.document.Fault;
import com.sun.tools.ws.wsdl.document.Input;
import com.sun.tools.ws.wsdl.document.Output;
import com.sun.xml.ws.api.addressing.AddressingVersion;
import org.w3c.dom.Element;

/**
 * @author Arun Gupta
 */
public class W3CAddressingExtensionHandler extends AbstractExtensionHandler {
    private final ProcessorEnvironment env;

    public W3CAddressingExtensionHandler(Map<String, AbstractExtensionHandler> extensionHandlerMap) {
        this(extensionHandlerMap, null);
    }

    public W3CAddressingExtensionHandler(Map<String, AbstractExtensionHandler> extensionHandlerMap, ProcessorEnvironment env) {
        super(extensionHandlerMap);
        this.env = env;
    }

    @Override
    public String getNamespaceURI() {
        return AddressingVersion.W3C.nsUri;
    }

    protected QName getActionQName() {
        return AddressingVersion.W3C.wsdlActionTag;
    }

    protected QName getWSDLExtensionQName() {
        return AddressingVersion.W3C.wsdlExtensionTag;
    }

    @Override
    public boolean handleBindingExtension(TWSDLParserContext context, TWSDLExtensible parent, Element e) {
        if (XmlUtil.matchesTagNS(e, getWSDLExtensionQName())) {
            context.push();
            context.registerNamespaces(e);

            // TODO: read UsingAddressing extensibility element and store
            // TODO: it as extension in "parent". It may be used to generate
            // TODO: @Action/@FaultAction later.

            context.pop();
            return true;
        }
        return false; // keep compiler happy
    }

    @Override
    public boolean handleInputExtension(TWSDLParserContext context, TWSDLExtensible parent, Element e) {
        String actionValue = XmlUtil.getAttributeNSOrNull(e, getActionQName());
        if (actionValue == null || actionValue.equals("")) {
            return warnEmptyAction(parent);
        }

        context.push();
        ((Input)parent).setAction(actionValue);
        context.pop();

        return true;
    }

    @Override
    public boolean handleOutputExtension(TWSDLParserContext context, TWSDLExtensible parent, Element e) {
        String actionValue = XmlUtil.getAttributeNSOrNull(e, getActionQName());
        if (actionValue == null || actionValue.equals("")) {
            return warnEmptyAction(parent);
        }

        context.push();
        ((Output)parent).setAction(actionValue);
        context.pop();

        return true;
    }

    @Override
    public boolean handleFaultExtension(TWSDLParserContext context, TWSDLExtensible parent, Element e) {
        String actionValue = XmlUtil.getAttributeNSOrNull(e, getActionQName());
        if (actionValue == null || actionValue.equals("")) {
            env.warn(WsdlMessages.localizableWARNING_FAULT_EMPTY_ACTION(parent.getNameValue(), parent.getWSDLElementName().getLocalPart(), parent.getParent().getNameValue()));
            return false; // keep compiler happy
        }

        context.push();
        ((Fault)parent).setAction(actionValue);
        context.pop();

        return true;
    }

    @Override
    public boolean handlePortExtension(TWSDLParserContext context, TWSDLExtensible parent, Element e) {
        return handleBindingExtension(context, parent, e);
    }

    private boolean warnEmptyAction(TWSDLExtensible parent) {
        env.warn(WsdlMessages.localizableWARNING_INPUT_OUTPUT_EMPTY_ACTION(parent.getWSDLElementName().getLocalPart(), parent.getParent().getNameValue()));
        return false; // keep compiler happy
    }
}
