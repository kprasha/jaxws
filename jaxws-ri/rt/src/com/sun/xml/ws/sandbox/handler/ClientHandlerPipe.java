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

import com.sun.xml.ws.binding.BindingImpl;
import com.sun.xml.ws.binding.soap.SOAPBindingImpl;
import com.sun.xml.ws.handler.HandlerPipe;
import com.sun.xml.ws.handler.MessageContextUtil;
import com.sun.xml.ws.sandbox.handler.HandlerChainCaller.Direction;
import com.sun.xml.ws.sandbox.handler.HandlerChainCaller.RequestOrResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.soap.SOAPException;
import javax.xml.ws.ProtocolException;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.Handler;
import java.util.Set;
import com.sun.xml.ws.api.message.Message;
import javax.xml.ws.handler.LogicalHandler;
import javax.xml.ws.handler.LogicalMessageContext;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import javax.xml.ws.soap.SOAPFaultException;

/**
 * Dummy ClientHandlerPipe
 * @author WS Development Team
 */
public class ClientHandlerPipe extends HandlerPipe{
    private BindingImpl binding;
    private HandlerChainCaller hcaller;
    
    /** Creates a new instance of ClientHandlerPipe */
    public ClientHandlerPipe(BindingImpl binding) {
        this.binding = binding;
        hcaller = new HandlerChainCaller(binding,binding.getHandlerChain());
        if(binding.getSOAPVersion() != null) {
            //set roles on HandlerChainCaller, if it is SOAP binding
            hcaller.setRoles(((SOAPBindingImpl)binding).getRoles());
        }
    }
    
    public HandlerChainCaller getHandlerChainCaller() {
        return hcaller;
    }
    public void setHandlerChainCaller(HandlerChainCaller hcaller) {
        this.hcaller= hcaller;
    }
    
    public Message process(Message msg) {
        // This is client-side pipe, so it should be request ( OUTBOUND Message).
        // TODO: MessageContext.MESSAGE_OUTBOUND_PROPERTY should be true
        // TODO: This is taken from SOAPMessageDispatcher.Make sure this is done somewhere before HandlerPipe is called.
        //       this is needed so that attachments are compied from RESPONSE_MESSAGE_ATTACHMEMTN PROPERTY
        //TODO: How do you know if its Oneway?
        boolean isOneWay = msg.getProperties().isOneWay;
        boolean handlerResult = true;
        if(hcaller.hasHandlers()) {
            try {
                handlerResult = callHandlersOnRequest(msg);
            } catch (ProtocolException pe) {
                handlerResult = false;
                if (MessageContextUtil.ignoreFaultInMessage(
                        msg.getProperties())) {
                    // ignore fault in this case and use exception
                    //RELOOK: code taken from SMD, should n't PE be deirectly given to client, why wrapping?
                    throw new WebServiceException(pe);
                }
            }
            // the only case where no message is sent
            if (!isOneWay && !handlerResult) {
                //RELOOK: 
                return msg;
            }
        }
        /*        
        try {
            // Call next Pipe.process() on msg        
            // Next Pipe Should be MUHeaderPipe
            // TODO: Do MUHeader Processing
        } catch (WebServiceException wse) { 
            //RELOOK: changed from SMD code
            hcaller.forceCloseHandlers(msg);
             throw wse;
        } catch (RuntimeException re) {
            //RELOOK: changed from SMD code
            hcaller.forceCloseHandlers(msg);
            throw re;
        }
        */
        // Who sets this?
        // TODO: MessageContext.MESSAGE_OUTBOUND_PROPERTY should be false
        // TODO: Make sure MessageContext.INBOUND_MESSAGE_ATTACHMENTS is populated.
        if (hcaller.hasHandlers()) {
            callHandlersOnResponse(msg);
        }
        
        return null;
        
    }
    
    protected boolean callHandlersOnRequest(Message msg) {
        HandlerChainCaller caller = getHandlerChainCaller();
        boolean responseExpected = msg.getProperties().isOneWay;
        return caller.callHandlers(Direction.OUTBOUND, RequestOrResponse.REQUEST, msg,
                responseExpected);
    }
    
    /*
     * Because a user's handler can throw a web service exception
     * (e.g., a ProtocolException), we need to wrap any such exceptions
     * here because the main catch clause assumes that WebServiceExceptions
     * are already wrapped.
     */
    protected boolean callHandlersOnResponse(Message msg) {
        HandlerChainCaller caller = getHandlerChainCaller();
        try {
            return caller.callHandlers(Direction.INBOUND,
                    RequestOrResponse.RESPONSE, msg, false);
        } catch (WebServiceException wse) {
            throw new WebServiceException(wse);
        }
    }
    
    
    
}
