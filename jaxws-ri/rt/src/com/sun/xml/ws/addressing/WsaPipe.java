/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License).  You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the license at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.xml.ws.addressing;

import com.sun.xml.ws.addressing.v200408.MemberSubmissionAddressingConstants;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.addressing.MemberSubmissionAddressingFeature;
import com.sun.xml.ws.api.model.SEIModel;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.PipeCloner;
import com.sun.xml.ws.api.pipe.helper.AbstractPipeImpl;
import com.sun.xml.ws.binding.SOAPBindingImpl;

/**
 * @author Arun Gupta
 */
public abstract class WsaPipe extends AbstractPipeImpl {
    final WSDLPort wsdlPort;
    final WSBinding binding;
    final WsaPipeHelper helper;
    final Pipe next;

    public WsaPipe(WSDLPort wsdlPort, WSBinding binding, Pipe next) {
        this.wsdlPort = wsdlPort;
        this.binding = binding;
        this.next = PipeCloner.clone(next);
        helper = getPipeHelper();
    }

    private WsaPipeHelper getPipeHelper() {
        if (wsdlPort == null) {
            if (binding.hasFeature(MemberSubmissionAddressingFeature.ID))
                return new com.sun.xml.ws.addressing.v200408.WsaPipeHelperImpl(wsdlPort, binding);
            else
                return new WsaPipeHelperImpl(wsdlPort, binding);
        }

        String ns = wsdlPort.getBinding().getAddressingVersion();
        if (ns != null && ns.equals(MemberSubmissionAddressingConstants.WSA_NAMESPACE_NAME))
            return new com.sun.xml.ws.addressing.v200408.WsaPipeHelperImpl(wsdlPort, binding);
        else
            return new WsaPipeHelperImpl(wsdlPort, binding);
    }
}
