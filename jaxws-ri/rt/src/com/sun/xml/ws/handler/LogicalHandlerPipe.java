/*
 * LogicalHandlerPipe.java
 *
 * Created on February 9, 2006, 11:12 PM
 *
 *
 */

package com.sun.xml.ws.handler;

import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.PipeCloner;
import com.sun.xml.ws.binding.BindingImpl;
import com.sun.xml.ws.handler.HandlerProcessor.Direction;
import java.util.ArrayList;
import java.util.List;
import javax.xml.ws.ProtocolException;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.LogicalHandler;
import javax.xml.ws.handler.LogicalMessageContext;
import javax.xml.ws.handler.MessageContext;


/**
 * TODO: Kohsuke put a comment that packet.isOneWay may be null
 * @author WS Development Team
 */
public class LogicalHandlerPipe extends HandlerPipe {
    
    private WSBinding binding;
    private LogicalHandlerProcessor processor;
    private List<LogicalHandler> logicalHandlers;
    private boolean remedyActionTaken = false;
    private final boolean isClient;
    
    
    /** Creates a new instance of LogicalHandlerPipe */
    public LogicalHandlerPipe(WSBinding binding, Pipe next, boolean isClient) {
        super(next);
        this.binding = binding;
        this.isClient = isClient;
    }
    
    
    /**
     * This constructor is used on client-side where, SOAPHandlerPipe is created
     * first and then a LogicalHandlerPipe is created with a handler to that
     * SOAPHandlerPipe.
     * With this handle, LogicalHandlerPipe can call
     * SOAPHandlerPipe.closeHandlers()
     */
    public LogicalHandlerPipe(WSBinding binding, Pipe next, HandlerPipe cousinPipe, boolean isClient) {
        super(next,cousinPipe);
        this.binding = binding;
        this.isClient = isClient;
    }
    
    /**
     * Copy constructor for {@link com.sun.xml.ws.api.pipe.Pipe#copy(com.sun.xml.ws.api.pipe.PipeCloner)}.
     */
    
    protected LogicalHandlerPipe(LogicalHandlerPipe that, PipeCloner cloner) {
        super(that,cloner);
        this.binding = that.binding;
        this.isClient = that.isClient;
    }
    
    @Override
            public Packet process( Packet packet) {
        // This is done here instead of the constructor, since User can change
        // the roles and handlerchain after a stub/proxy is created.
        setUpProcessor();
        
        if(logicalHandlers.isEmpty()) {
            return next.process(packet);
        }
        
        MessageContextImpl msgContext = new MessageContextImpl(packet);
        LogicalMessageContextImpl context =  new LogicalMessageContextImpl(binding,packet,msgContext);
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
            try {
                // Call handlers on Request
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
            //Update Packet Properties
            context.updatePacket();
            msgContext.fill(packet);
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
            msgContext = new MessageContextImpl(reply);
            context =  new LogicalMessageContextImpl(binding,reply,msgContext);
            try {
                //If null, it is oneway
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
            close(msgContext);
        }
        //Update Packet Properties
        context.updatePacket();
        msgContext.fill(reply);
        return reply;
        
        
    }
    
    /**
     * Close SOAPHandlers first and then LogicalHandlers on Client
     * Close LogicalHandlers first and then SOAPHandlers on Server
     */
    public void close(MessageContext msgContext){
        if(isClient){
            //cousinPipe is null in XML/HTTP Binding
            if(cousinPipe != null){
                // Close SOAPHandlerPipe
                cousinPipe.closeCall(msgContext);
            }
            if(processor != null)
                closeLogicalHandlers(msgContext);
        } else {
            if(binding.getSOAPVersion() != null) {
                //SOAPHandlerPipe will drive the closing of LogicalHandlerPipe
            } else {
                if(processor != null)
                    closeLogicalHandlers(msgContext);
            }
        }
        
        
        
        
    }
    /**
     * This is called from cousinPipe.
     * Close this Pipes's handlers.
     */
    public void closeCall(MessageContext msgContext){
        closeLogicalHandlers(msgContext);
    }
    
    //TODO:
    private void closeLogicalHandlers(MessageContext msgContext){
        if(processor == null)
            return;
        if(remedyActionTaken) {
            //Close only invoked handlers in the chain
            if(isClient){
                //CLIENT-SIDE
                processor.closeHandlers(msgContext,processor.getIndex(),0);
            } else {
                //SERVER-SIDE
                processor.closeHandlers(msgContext,processor.getIndex(),logicalHandlers.size()-1);
            }
        } else {
            //Close all handlers in the chain
            if(isClient){
                //CLIENT-SIDE
                processor.closeHandlers(msgContext,logicalHandlers.size()-1,0);
            } else {
                //SERVER-SIDE
                processor.closeHandlers(msgContext,0,logicalHandlers.size()-1);
            }
        }
    }
    
    /**
     * TODO:
     * @param cloner
     * @return
     */
    public Pipe copy(PipeCloner cloner) {
        return new LogicalHandlerPipe(this,cloner);
    }
    
    private void setUpProcessor() {
        // Take a snapshot, User may change chain after invocation, Same chain
        // should be used for the entire MEP
        logicalHandlers = new ArrayList<LogicalHandler>();
        logicalHandlers.addAll(((BindingImpl)binding).getLogicalHandlerChain());
        processor = new LogicalHandlerProcessor(binding,logicalHandlers,isClient);
    }
    
}
