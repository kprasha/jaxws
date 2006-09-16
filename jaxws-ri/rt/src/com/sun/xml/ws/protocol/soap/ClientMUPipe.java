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
package com.sun.xml.ws.protocol.soap;

import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.TubeCloner;
import com.sun.xml.ws.api.pipe.NextAction;
import com.sun.xml.ws.api.pipe.helper.AbstractTubeImpl;
import com.sun.xml.ws.client.HandlerConfiguration;

import javax.xml.namespace.QName;
import javax.xml.ws.soap.SOAPFaultException;
import java.util.Set;

/**
 * @author Rama Pulavarthi
 */

public class ClientMUPipe extends MUPipe {

    // TODO remove after moving to fiber
    private Pipe next;

    public ClientMUPipe(WSBinding binding, Pipe next) {
        super(binding, next);
        this.next = next;
    }

    protected ClientMUPipe(ClientMUPipe that, TubeCloner cloner) {
        super(that,cloner);
        this.next = that.next;
    }

    /**
     * Do MU Header Processing on incoming message (repsonse)
     * @return
     *         if all the headers in the packet are understood, returns the packet.
     * @throws SOAPFaultException
     *         if all the headers in the packet are not understood, throws SOAPFaultException
     */
    // TODO remove after moving to fiber
    public Packet process(Packet packet) {
        Packet reply = next.process(packet);
        //Oneway
        if(reply.getMessage() == null) {
            return reply;
        }
        HandlerConfiguration handlerConfig = packet.handlerConfig;
        Set<QName> misUnderstoodHeaders = getMisUnderstoodHeaders(reply.getMessage().getHeaders(),
                handlerConfig.getRoles(),handlerConfig.getKnownHeaders());
        if((misUnderstoodHeaders == null) || misUnderstoodHeaders.isEmpty()) {
            return reply;
        }
        throw createMUSOAPFaultException(misUnderstoodHeaders);

    }

    public NextAction processRequest(Packet request) {
        return doInvoke(super.next, request);
    }

    public NextAction processResponse(Packet response) {
        HandlerConfiguration handlerConfig = response.handlerConfig;
        Set<QName> misUnderstoodHeaders = getMisUnderstoodHeaders(response.getMessage().getHeaders(),
                handlerConfig.getRoles(),handlerConfig.getKnownHeaders());
        if((misUnderstoodHeaders == null) || misUnderstoodHeaders.isEmpty()) {
            return doReturnWith(response);
        }
        // TODO should we convert to fault message
        throw createMUSOAPFaultException(misUnderstoodHeaders);
    }

    public AbstractTubeImpl copy(TubeCloner cloner) {
        return new ClientMUPipe(this,cloner);
    }

}
