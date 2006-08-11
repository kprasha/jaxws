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
 $Id: MemberSubmissionAddressingExtensionHandler.java,v 1.1.2.1 2006-08-11 20:03:41 arungupta Exp $

 Copyright (c) 2006 Sun Microsystems, Inc.
 All rights reserved.
*/

package com.sun.tools.ws.wsdl.parser;

import java.util.Map;

import com.sun.tools.ws.api.wsdl.TWSDLParserContext;
import com.sun.tools.ws.api.wsdl.TWSDLExtensible;
import com.sun.xml.ws.wsdl.parser.MemberSubmissionAddressingConstants;
import org.w3c.dom.Element;

/**
 * @author Arun Gupta
 */
public class MemberSubmissionAddressingExtensionHandler extends W3CAddressingExtensionHandler {
    public MemberSubmissionAddressingExtensionHandler(Map<String, AbstractExtensionHandler> extensionHandlerMap) {
        super(extensionHandlerMap);
    }

    @Override
    public String getNamespaceURI() {
        return MemberSubmissionAddressingConstants.WSA_NAMESPACE_WSDL_NAME;
    }

    @Override
    public boolean handlePortExtension(TWSDLParserContext context, TWSDLExtensible parent, Element e) {
        // ignore any extension elements
        return false;
    }

}
