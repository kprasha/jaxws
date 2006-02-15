/*
 * SOAPHandlerPipe.java
 *
 * Created on February 9, 2006, 11:12 PM
 *
 *
 */

package com.sun.xml.ws.sandbox.handler;

import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.PipeCloner;
import com.sun.xml.ws.binding.BindingImpl;
import com.sun.xml.ws.binding.soap.SOAPBindingImpl;
import com.sun.xml.ws.handler.HandlerPipe;
import com.sun.xml.ws.handler.MessageContextUtil;
import com.sun.xml.ws.sandbox.handler.HandlerProcessor.Direction;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.namespace.QName;
import javax.xml.ws.ProtocolException;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

/**
 * TODO: Kohsuke put a comment that packet.isOneWay may be null
 * @author WS Development Team
 */
public class SOAPHandlerPipe extends HandlerPipe {
    
    private WSBinding binding;
    private SOAPHandlerProcessor processor;
    private List<SOAPHandler> soapHandlers;
    protected Set<String> roles;
    private boolean remedyActionTaken = false;
    private final boolean isClient;
    
    /** Creates a new instance of SOAPHandlerPipe */
    public SOAPHandlerPipe(WSBinding binding, Pipe next, boolean isClient) {
        super(next);
        if(binding.getSOAPVersion() != null) {
            // SOAPHandlerPipe should n't be used for bindings other than SOAP.
            // TODO: throw Exception
        }        
        this.binding = binding;
        this.isClient = isClient;
    }
    
    // Handle to LogicalHandlerPipe means its used on SERVER-SIDE
    /**
     * This constructor is used on client-side where, LogicalHandlerPipe is created
     * first and then a SOAPHandlerPipe is created with a handler to that
     * LogicalHandlerPipe.
     * With this handle, SOAPHandlerPipe can call LogicalHandlerPipe.closeHandlers()
     */
    public SOAPHandlerPipe(WSBinding binding, Pipe next, HandlerPipe cousinPipe, boolean isClient) {
        super(next,cousinPipe);
        this.binding = binding;
        this.isClient = isClient;
    }
    
    /**
     * Copy constructor for {@link com.sun.xml.ws.api.pipe.Pipe#copy(com.sun.xml.ws.api.pipe.PipeCloner)}.
     */
    protected SOAPHandlerPipe(SOAPHandlerPipe that, PipeCloner cloner) {
        super(that,cloner);
        this.isClient = that.isClient;
    }
    
    @Override
    public Packet process( Packet packet) {
        // This is done here instead of the constructor, since User can change
        // the roles and handlerchain after a stub/proxy is created.
        setUpProcessor();
        
        if(soapHandlers.isEmpty()) {
            return next.process(packet);
        }
        MessageContext msgContext = new MessageContextImpl(packet);
        try {
            boolean isOneWay = packet.isOneWay;            
            SOAPMessageContextImpl context =  new SOAPMessageContextImpl(binding,packet,msgContext);
            context.setRoles(roles);
            boolean handlerResult = false;
            // Call handlers on Request
            try {
                if(!soapHandlers.isEmpty()) {
                    if(isClient) {
                        //CLIENT-SIDE
                        handlerResult = processor.callHandlersRequest(Direction.OUTBOUND,context,!isOneWay);
                    } else {
                        //SERVER-SIDE
                        handlerResult = processor.callHandlersRequest(Direction.INBOUND,context,!isOneWay);
                    }
                }
            } catch (ProtocolException pe) {
                handlerResult = false;
                if (MessageContextUtil.ignoreFaultInMessage(msgContext)) {
                    // ignore fault in this case and use exception
                    throw pe;
                }
            }
            
            // the only case where no message is sent
            if (!isOneWay && !handlerResult) {
                remedyActionTaken = true;
                //TODO: return packet
                return packet;
            }
            
            // Call next Pipe.process() on msg
            Packet reply = next.process(packet);
                        
            //TODO: For now create again
            msgContext = new MessageContextImpl(packet);
            
            context =  new SOAPMessageContextImpl(binding,packet,msgContext);
            context.setRoles(roles);
            // Call handlers on Response
            if(isClient) {
                //CLIENT-SIDE
                processor.callHandlersResponse(Direction.INBOUND,context);
            } else {
                //SERVER-SIDE
                processor.callHandlersResponse(Direction.OUTBOUND,context);
            }
            
            return reply;
        } finally {
            close(msgContext);
        }
    }

    /**
     * Close SOAPHandlers first and then LogicalHandlers
     */
    public void close(MessageContext msgContext){
        if(processor != null)
            closeSOAPHandlers(msgContext);
        if(cousinPipe != null){
            // Close LogicalHandlers
            cousinPipe.close(msgContext);            
        }        
    }
    
    //TODO:
    private void closeSOAPHandlers(MessageContext msgContext){
        if(remedyActionTaken){
          //Close only invoked handlers in the chain
          if(isClient){
              //CLIENT-SIDE
              processor.closeHandlers(msgContext,0,processor.getIndex());              
          } else {
              //SERVER-SIDE
              processor.closeHandlers(msgContext,soapHandlers.size()-1,processor.getIndex());
          }
      } else {
          //Close all handlers in the chain
          processor.closeHandlers(msgContext,soapHandlers.size()-1,0);
      }
    }
    
    /**
     * TODO:
     * @param cloner
     * @return
     */
    public Pipe copy(PipeCloner cloner) {
        return new SOAPHandlerPipe(this,cloner);
    }
    
    private void setUpProcessor() {
        // Take a snapshot, User may change chain after invocation, Same chain 
        // should be used for the entire MEP
        soapHandlers = new ArrayList<SOAPHandler>();
        soapHandlers.addAll(((BindingImpl)binding).getSOAPHandlerChain());
        roles = new HashSet<String>();
        roles.addAll(((SOAPBindingImpl)binding).getRoles());
        processor = new SOAPHandlerProcessor(binding,soapHandlers);
    }
    
}
