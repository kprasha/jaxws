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
package com.sun.xml.ws.protocol.soap;

import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.PipeCloner;
import com.sun.xml.ws.client.HandlerConfiguration;
import com.sun.xml.ws.binding.BindingImpl;
import javax.xml.namespace.QName;
import java.util.Set;

/**
 * @author Rama Pulavarthi
 */

public class ServerMUPipe extends MUPipe {
    private HandlerConfiguration handlerConfig;
    public ServerMUPipe(WSBinding binding, Pipe next) {
        super(binding, next);
        //On Server, HandlerConfiguration does n't change after publish.
        handlerConfig = ((BindingImpl)binding).getHandlerConfig();
    }

    protected ServerMUPipe(ServerMUPipe that, PipeCloner cloner) {
        super(that,cloner);
        handlerConfig = that.handlerConfig;
    }

    /**
     * Do MU Header Processing on incoming message (request)
     * @return
     *      if all the headers in the packet are understood, returns the next Pipe.process()
     *      if all the headers in the packet are not understood, returns a Packet with SOAPFault Message
     */
    @Override
    public Packet process(Packet packet) {

        Set<QName> misUnderstoodHeaders = getMisUnderstoodHeaders(packet.getMessage().getHeaders(),
                handlerConfig.getRoles(),handlerConfig.getKnownHeaders());
        if((misUnderstoodHeaders == null)  || misUnderstoodHeaders.isEmpty()) {
            return next.process(packet);
        }
        return packet.createResponse(createMUSOAPFaultMessage(misUnderstoodHeaders));
    }

    public Pipe copy(PipeCloner cloner) {
            return new ServerMUPipe(this,cloner);
    }

}
