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

package com.sun.xml.ws.handler;

import com.sun.istack.Nullable;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.PipeCloner;
import com.sun.xml.ws.api.pipe.helper.AbstractFilterPipeImpl;
import com.sun.xml.ws.handler.HandlerProcessor.Direction;

import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.MessageContext;

/**
 * @author WS Development team
 */

public abstract class HandlerPipe extends AbstractFilterPipeImpl {
    
    /**
     * handle hold reference to other Pipe for inter-pipe communication
     */
    protected HandlerPipe cousinPipe;
    protected final boolean isClient;
    protected HandlerProcessor processor;
    protected boolean remedyActionTaken = false;

    private final @Nullable WSDLPort port;
    
    
    public HandlerPipe(Pipe next, WSDLPort port, boolean isClient) {
        super(next);
        this.port = port;
        this.isClient = isClient;
    }
    
    public HandlerPipe(Pipe next, HandlerPipe cousinPipe, boolean isClient) {
        super(next);
        this.cousinPipe = cousinPipe;
        this.isClient = isClient;
        if(cousinPipe != null)
            this.port = cousinPipe.port;
        else
            this.port = null;
    }
    
    /**
     * Copy constructor for {@link Pipe#copy(PipeCloner)}.
     */
    protected HandlerPipe(HandlerPipe that, PipeCloner cloner) {
        super(that,cloner);
        this.cousinPipe = that.cousinPipe;
        this.isClient = that.isClient;
        this.port = that.port;
    }
    
    public Packet process( Packet packet) {
        // This check is done to cover handler returning false in Oneway request
        Boolean handleFalse = (Boolean) packet.invocationProperties.get(HandlerProcessor.HANDLE_FALSE_PROPERTY);
        if(handleFalse != null && handleFalse){
            // Cousin HandlerPipe returned false during Oneway Request processing.
            // Dont call handlers and dispatch the message.
            remedyActionTaken = true;
            return next.process(packet);
        }

        // This is done here instead of the constructor, since User can change
        // the roles and handlerchain after a stub/proxy is created.
        setUpProcessor();

        if(isHandlerChainEmpty()) {
            return next.process(packet);
        }

        MessageUpdatableContext context = getContext(packet);
        boolean isOneWay = checkOneWay(packet);
        Packet reply;
        try {
            // Call handlers on Request
            boolean handlerResult = callHandlersOnRequest(context, isOneWay);
            //Update Packet with user modifications
            context.updatePacket();

            // the only case where no message is sent
            if (!isOneWay && !handlerResult) {
                return packet;
            }
            // Call next Pipe.process() on msg
            reply = next.process(packet);

            //RELOOK: For now create again
            context =  getContext(reply);

            //TODO: Server-side oneway is not correct
            //TODO: HandleFault incorrect

            //If null, it is oneway
            if(reply.getMessage()!= null){
                if(!isClient) {
                    if(reply.getMessage().isFault()) {
                        //handleFault() is called on handlers
                        processor.addHandleFaultProperty(context);
                    }
                }
            }
            if(reply.getMessage()!= null){
                // Call handlers on Response
                callHandlersOnResponse(context);
            }
        } finally {
            close(context.getMessageContext());
        }
        //Update Packet with user modifications
        context.updatePacket();

        return reply;
    }

    private boolean checkOneWay(Packet packet) {
        if (port != null) {
            /* we can determine this value from WSDL */
            return packet.getMessage().isOneWay(port);
        } else {
            /*
              otherwise use this value as an approximation, since this carries
              the appliation's intention --- whether it was invokeOneway vs invoke,etc.
             */
            return (packet.expectReply != null ? packet.expectReply : false);
        }
    }

    private boolean callHandlersOnRequest(MessageUpdatableContext context, boolean isOneWay){

        boolean handlerResult = false;
        try {
            if(isClient) {
                //CLIENT-SIDE
                handlerResult = processor.callHandlersRequest(Direction.OUTBOUND,context,!isOneWay);
            } else {
                //SERVER-SIDE
                handlerResult = processor.callHandlersRequest(Direction.INBOUND,context,!isOneWay);
            }
        } catch(WebServiceException wse) {
            remedyActionTaken = true;
            //no rewrapping
            throw wse;
        } catch(RuntimeException re){
            remedyActionTaken = true;
            if(isClient){
                throw new WebServiceException(re);
            } else {
                throw re;
            }
        }
        if(!handlerResult) {
                remedyActionTaken = true;
        }
        return handlerResult;
    }

    private void callHandlersOnResponse(MessageUpdatableContext context){
        try {
            if(isClient) {
                //CLIENT-SIDE
                processor.callHandlersResponse(Direction.INBOUND,context);
            } else {
                //SERVER-SIDE
                processor.callHandlersResponse(Direction.OUTBOUND,context);
            }
        } catch(WebServiceException wse) {
            //no rewrapping
            throw wse;
        } catch(RuntimeException re){
            if(isClient){
                throw new WebServiceException(re);
            } else {
                throw re;
            }
        }
    }

    abstract void setUpProcessor();
    abstract boolean isHandlerChainEmpty();
    abstract MessageUpdatableContext getContext(Packet p);
    
    /**
     * Close SOAPHandlers first and then LogicalHandlers on Client
     * Close LogicalHandlers first and then SOAPHandlers on Server
     */
    public abstract void close(MessageContext msgContext);
    
    /**
     * This is called from cousinPipe.
     * Close this Pipes's handlers.
     */
    public abstract void closeCall(MessageContext msgContext);
    
    
    protected HandlerPipeExchange exchange;
    /**
     * This class is used primarily to exchange information or status between
     * LogicalHandlerPipe and SOAPHandlerPipe
     */
    
    public class HandlerPipeExchange {
        //TODO: get the requirements from different scenarios
        String dummy;
    }
    
}
