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
 $Id: WsaPipe.java,v 1.1.2.1 2006-08-18 21:56:14 arungupta Exp $

 Copyright (c) 2006 Sun Microsystems, Inc.
 All rights reserved.
*/

package com.sun.xml.ws.addressing;

import com.sun.xml.ws.api.pipe.helper.AbstractPipeImpl;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.PipeCloner;
import com.sun.xml.ws.api.model.SEIModel;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.WSBinding;

/**
 * @author Arun Gupta
 */
public abstract class WsaPipe extends AbstractPipeImpl {
    final SEIModel seiModel;
    final WSDLPort wsdlPort;
    final WSBinding binding;
    final WsaPipeHelper helper;
    final Pipe next;

    public WsaPipe(WSDLPort wsdlPort, WSBinding binding, Pipe next) {
        this(null, wsdlPort, binding, next);
    }

    public WsaPipe(SEIModel seiModel, WSDLPort wsdlPort, WSBinding binding, Pipe next) {
        this.seiModel = seiModel;
        this.wsdlPort = wsdlPort;
        this.binding = binding;
        this.next = PipeCloner.clone(next);
        helper = getPipeHelper();
    }

    private WsaPipeHelper getPipeHelper() {
        return new AddressingWsaPipeHelperImpl(seiModel, wsdlPort, binding);
    }
}
