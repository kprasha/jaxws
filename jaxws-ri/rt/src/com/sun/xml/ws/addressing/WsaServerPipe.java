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
