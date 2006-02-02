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

package com.sun.xml.ws.sandbox.handler;

import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.binding.soap.SOAPBindingImpl;
import com.sun.xml.ws.handler.HandlerPipe;
import com.sun.xml.ws.handler.MessageContextUtil;
import com.sun.xml.ws.sandbox.handler.HandlerChainCaller.ContextHolder;
import com.sun.xml.ws.sandbox.handler.HandlerChainCaller.Direction;
import com.sun.xml.ws.sandbox.handler.HandlerChainCaller.RequestOrResponse;
import javax.xml.ws.ProtocolException;
import javax.xml.ws.WebServiceException;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Packet;

/**
 * Dummy ClientHandlerPipe
 * @author WS Development Team
 */
public class ClientHandlerPipe extends HandlerPipe{
    private WSBinding binding;
    private HandlerChainCaller hcaller;
    private Pipe nextPipe;
    /** Creates a new instance of ClientHandlerPipe */
    public ClientHandlerPipe(WSBinding binding, Pipe nextPipe) {
        this.binding = binding;
        this.nextPipe = nextPipe;
        /*
         *Reason for commenting:
         * Client can set a new HandlerChain on Binding after the ClientPipe is 
         * created.
         * When a chain is changed , the handler pipe should use new chain, by 
         * calling getHandlerChainCaller on the binding. HandlerChainCaller in
         * BindingImpl, SOAPBindingImpl is fromt he old pacakge and server-runtime
         * depends on it.
         * After server-runtime uses new handler runtime, it can change there and
         * HandlerChainCaller will be taken from BindingImpl, befor processing
         */
        /*
        hcaller = new HandlerChainCaller(binding,binding.getHandlerChain());
        if(binding.getSOAPVersion() != null) {
            //set roles on HandlerChainCaller, if it is SOAP binding
            hcaller.setRoles(((SOAPBindingImpl)binding).getRoles());
        } */
    }

    public HandlerChainCaller getHandlerChainCaller() {
        return hcaller;
    }
    public void setHandlerChainCaller(HandlerChainCaller hcaller) {
        this.hcaller= hcaller;
    }

    public Packet process(Packet packet) {
        /*
         *Remove this code and put it in constructor, after Server Runtime is changed.
         *See constructor
         */
        hcaller = new HandlerChainCaller(binding,binding.getHandlerChain());
        if(binding.getSOAPVersion() != null) {
            //set roles on HandlerChainCaller, if it is SOAP binding
            hcaller.setRoles(((SOAPBindingImpl)binding).getRoles());
        }



        // This is client-side pipe, so it should be request ( OUTBOUND Message).
        boolean isOneWay = packet.isOneWay;
        ContextHolder ch = new ContextHolder(binding,packet);
        boolean handlerResult = true;
        if(hcaller.hasHandlers()) {
            try {
                handlerResult = callHandlersOnRequest(ch);
            } catch (ProtocolException pe) {
                handlerResult = false;
                if (MessageContextUtil.ignoreFaultInMessage(
                        ch.getMC())) {
                    // ignore fault in this case and use exception

                    //RELOOK: code taken from SMD, old code wraps in WSE, is it needed?
                    //        Changed to throw PE directly
                    throw pe;
                }
            }
            // the only case where no message is sent
            if (!isOneWay && !handlerResult) {
                //RELOOK: 
                return ch.getMessage();
            }
        }

        // Call next Pipe.process() on msg
        Packet reply;
        try {
            reply = nextPipe.process(ch.getMessage());
            // Next Pipe Should be MUHeaderPipe
            // TODO: Do MUHeader Processing
        } catch (WebServiceException wse) {
            //RELOOK: changed from SMD code
            hcaller.forceCloseHandlers(ch);
             throw wse;
        } catch (RuntimeException re) {
            //RELOOK: changed from SMD code
            hcaller.forceCloseHandlers(ch);
            throw re;
        }
        ch = new ContextHolder(binding,reply);
        if (hcaller.hasHandlers()) {
            callHandlersOnResponse(ch);
        }

        return ch.getMessage();
    }

    protected boolean callHandlersOnRequest(ContextHolder ch) {
        HandlerChainCaller caller = getHandlerChainCaller();
        // TODO: isOneWay may null. This code can't be right - KK
        boolean responseExpected = ch.getMessage().isOneWay;
        return caller.callHandlers(Direction.OUTBOUND, RequestOrResponse.REQUEST, ch,
                responseExpected);
    }

    /*
    * Because a user's handler can throw a web service exception
    * (e.g., a ProtocolException), we need to wrap any such exceptions
    * here because the main catch clause assumes that WebServiceExceptions
    * are already wrapped.
    */
    protected boolean callHandlersOnResponse(ContextHolder ch) {
        HandlerChainCaller caller = getHandlerChainCaller();
        try {
            return caller.callHandlers(Direction.INBOUND,
                    RequestOrResponse.RESPONSE, ch, false);
        } catch (WebServiceException wse) {
            //RELOOK: why wrapping?
            throw new WebServiceException(wse);
        }
    }



}
