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
 $Id: W3CAddressingWSDLGeneratorExtension.java,v 1.1.2.1 2006-08-11 21:54:30 arungupta Exp $

 Copyright (c) 2006 Sun Microsystems, Inc.
 All rights reserved.
*/

package com.sun.xml.ws.wsdl.writer;

import java.lang.reflect.Method;

import javax.xml.ws.Action;
import javax.xml.ws.FaultAction;
import javax.xml.ws.soap.SOAPBinding;

import com.sun.istack.NotNull;
import com.sun.xml.txw2.TypedXmlWriter;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.model.CheckedException;
import com.sun.xml.ws.api.model.SEIModel;
import com.sun.xml.ws.api.server.Container;
import com.sun.xml.ws.api.wsdl.writer.WSDLGeneratorExtension;
import com.sun.xml.ws.wsdl.parser.W3CAddressingConstants;

/**
 * @author Arun Gupta
 */
public class W3CAddressingWSDLGeneratorExtension extends WSDLGeneratorExtension {
    private boolean enabled;

    @Override
    public void start(@NotNull TypedXmlWriter root, @NotNull SEIModel model, @NotNull WSBinding binding, @NotNull Container container) {
        enabled = binding.hasFeature(SOAPBinding.ADDRESSING_FEATURE);

        if (!enabled)
            return;

        root._namespace(W3CAddressingConstants.WSA_NAMESPACE_WSDL_NAME, W3CAddressingConstants.WSA_NAMESPACE_WSDL_PREFIX);
    }

    @Override
    public void addOperationInputExtension(TypedXmlWriter input, Method method) {
        if (!enabled)
            return;

        Action a = method.getAnnotation(Action.class);
        if (a != null && !a.input().equals("")) {
            addAttribute(input, a.input());
        }
    }

    @Override
    public void addOperationOutputExtension(TypedXmlWriter output, Method method) {
        if (!enabled)
            return;

        Action a = method.getAnnotation(Action.class);
        if (a != null && !a.output().equals("")) {
            addAttribute(output, a.output());
        }
    }

    @Override
    public void addOperationFaultExtension(TypedXmlWriter fault, Method method, CheckedException ce) {
        if (!enabled)
            return;

        Action a = method.getAnnotation(Action.class);
        Class[] exs = method.getExceptionTypes();

        if (exs == null)
            return;

        if (a != null && a.fault() != null) {
            for (FaultAction fa : a.fault()) {
                if (fa.className().getName().equals(ce.getExcpetionClass().getName())) {
                    addAttribute(fault, fa.value());
                    return;
                }
            }
        }
    }

    private void addAttribute(TypedXmlWriter writer, String attrValue) {
        writer._attribute(W3CAddressingConstants.WSAW_ACTION_QNAME, attrValue);
    }

    @Override
    public void addBindingExtension(TypedXmlWriter binding) {
        if (!enabled)
            return;

        UsingAddressing ua = binding._element(W3CAddressingConstants.WSAW_USING_ADDRESSING_QNAME, UsingAddressing.class);
    }
}
