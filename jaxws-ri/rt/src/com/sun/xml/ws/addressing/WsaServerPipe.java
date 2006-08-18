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
 $Id: WsaServerPipe.java,v 1.1.2.1 2006-08-18 21:56:15 arungupta Exp $

 Copyright (c) 2006 Sun Microsystems, Inc.
 All rights reserved.
*/

package com.sun.xml.ws.addressing;

import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.model.SEIModel;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.PipeCloner;

/**
 * @author Arun Gupta
 */
public class WsaServerPipe extends WsaPipe {
    public WsaServerPipe(SEIModel seiModel, WSDLPort wsdlPort, WSBinding binding, Pipe next) {
        super(seiModel, wsdlPort, binding, next);
    }

    public Pipe copy(PipeCloner cloner) {
        WsaServerPipe that = new WsaServerPipe(seiModel, wsdlPort, binding, next);
        cloner.add(this, that);

        return that;
    }

    public Packet process(Packet packet) {
        Packet p = helper.readServerInboundHeaders(packet);
        p = next.process(p);
        p = helper.writeServerOutboundHeaders(p);

        return p;
    }
}
