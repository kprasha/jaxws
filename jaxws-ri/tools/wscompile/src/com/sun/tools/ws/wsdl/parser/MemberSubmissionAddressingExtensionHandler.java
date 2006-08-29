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
 $Id: MemberSubmissionAddressingExtensionHandler.java,v 1.1.2.4 2006-08-29 19:34:18 arungupta Exp $

 Copyright (c) 2006 Sun Microsystems, Inc.
 All rights reserved.
*/

package com.sun.tools.ws.wsdl.parser;

import java.util.Map;

import javax.xml.namespace.QName;

import com.sun.tools.ws.api.wsdl.TWSDLExtensible;
import com.sun.tools.ws.api.wsdl.TWSDLParserContext;
import com.sun.tools.ws.processor.util.ProcessorEnvironment;
import com.sun.xml.ws.addressing.v200408.MemberSubmissionAddressingConstants;
import org.w3c.dom.Element;

/**
 * @author Arun Gupta
 */
public class MemberSubmissionAddressingExtensionHandler extends W3CAddressingExtensionHandler {
    public MemberSubmissionAddressingExtensionHandler(Map<String, AbstractExtensionHandler> extensionHandlerMap) {
        super(extensionHandlerMap);
    }

    public MemberSubmissionAddressingExtensionHandler(Map<String, AbstractExtensionHandler> extensionHandlerMap, ProcessorEnvironment env) {
        super(extensionHandlerMap, env);
    }

    @Override
    public String getNamespaceURI() {
        return MemberSubmissionAddressingConstants.WSA_NAMESPACE_WSDL_NAME;
    }

    protected QName getActionQName() {
        return MemberSubmissionAddressingConstants.WSAW_ACTION_QNAME;
    }

    protected QName getWSDLExtensionQName() {
        return MemberSubmissionAddressingConstants.WSAW_USING_ADDRESSING_QNAME;
    }

    @Override
    public boolean handlePortExtension(TWSDLParserContext context, TWSDLExtensible parent, Element e) {
        // ignore any extension elements
        return false;
    }

}
