/*
 * Copyright (c) 2006 Sun Microsystems Corporation. All Rights Reserved.
 */
package com.sun.xml.ws.handler;

import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.PipeCloner;
import com.sun.xml.ws.api.pipe.helper.AbstractPipeImpl;
import com.sun.xml.ws.api.pipe.helper.AbstractFilterPipeImpl;
import com.sun.xml.ws.handler.HandlerProcessor.Direction;
import java.util.List;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.Handler;
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
    
    
    public HandlerPipe(Pipe next, boolean isClient) {
        super(next);
        this.isClient = isClient;
    }
    
    public HandlerPipe(Pipe next, HandlerPipe cousinPipe, boolean isClient) {
        super(next);
        this.cousinPipe = cousinPipe;
        this.isClient = isClient;
    }
    
    /**
     * Copy constructor for {@link com.sun.xml.ws.api.pipe.Pipe#copy(com.sun.xml.ws.api.pipe.PipeCloner)}.
     */
    protected HandlerPipe(HandlerPipe that, PipeCloner cloner) {
        super(that,cloner);
        this.isClient = that.isClient;
    }
    
    public Packet process( Packet packet) {
        // This is done here instead of the constructor, since User can change
        // the roles and handlerchain after a stub/proxy is created.
        setUpProcessor();
        
        if(isHandlerChainEmpty()) {
            return next.process(packet);
        }
        MessageUpdatableContext context = getContext(packet);
        
        // Doing this just for Spec compliance
        // This check is done to cover handler returning false in Oneway request
        boolean handleFalse = processor.checkHandlerFalseProperty(context);
        if(handleFalse){
            // Cousin HandlerPipe returned false during Oneway Request processing.
            // Dont call handlers and dispatch the message.
            remedyActionTaken = true;
            return next.process(packet);
        }
        
        Packet reply;
        try {
            boolean isOneWay = (packet.isOneWay== null?false:packet.isOneWay);
            boolean handlerResult = false;
            
            // Call handlers on Request
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
            
            //Update Packet with user modifications
            context.updatePacket();
            
            
            if(!handlerResult) {
                remedyActionTaken = true;
            }
            // the only case where no message is sent
            if (!isOneWay && !handlerResult) {
                return packet;
            }
            
            // Call next Pipe.process() on msg
            reply = next.process(packet);
            
            //TODO: For now create again
            context =  getContext(reply);
            
            //If null, it is oneway
            try {
                if(reply.getMessage()!= null){
                    if(!isClient) {
                        if(reply.getMessage().isFault()) {
                            //handleFault() is called on handlers
                            processor.addHandleFaultProperty(context);
                        }
                    }
                    // Call handlers on Response
                    if(isClient) {
                        //CLIENT-SIDE
                        processor.callHandlersResponse(Direction.INBOUND,context);
                    } else {
                        //SERVER-SIDE
                        processor.callHandlersResponse(Direction.OUTBOUND,context);
                    }
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
            
        } finally {
            close(context.getMessageContext());
        }
        //Update Packet with user modifications
        context.updatePacket();
        
        return reply;
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
