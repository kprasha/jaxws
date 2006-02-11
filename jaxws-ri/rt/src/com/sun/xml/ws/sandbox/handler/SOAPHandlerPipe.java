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
    
    /** Creates a new instance of SOAPHandlerPipe */
    // No handle to LogicalHandlerPipe means its used on CLIENT-SIDE 
    public SOAPHandlerPipe(WSBinding binding, Pipe next) {
        super(next);
        if(binding.getSOAPVersion() != null) {
            // SOAPHandlerPipe should n't be used for bindings other than SOAP.
            // TODO: throw Exception
        }        
        this.binding = binding;        
    }
    
    // Handle to SOAPHandlerPipe means its used on SERVER-SIDE
    /**
     * This constructor is used on client-side where, SOAPHandlerPipe is created
     * first and then a SOAPHandlerPipe is created with a handler to that
     * SOAPHandlerPipe.
     * With this handle, SOAPHandlerPipe can call SOAPHandlerPipe.closeHandlers()
     */
    public SOAPHandlerPipe(WSBinding binding, Pipe next, HandlerPipe cousinPipe) {
        super(next,cousinPipe);
        this.binding = binding;
    }
    
    /**
     * Copy constructor for {@link com.sun.xml.ws.api.pipe.Pipe#copy(com.sun.xml.ws.api.pipe.PipeCloner)}.
     */
    protected SOAPHandlerPipe(HandlerPipe that, PipeCloner cloner) {
        super(that,cloner);
    }
    
    @Override
    public Packet process( Packet packet) {
        // This is done here instead of the constructor, since User can change
        // the roles and handlerchain after a stub/proxy is created.
        setUpProcessor();
        try {
            boolean isOneWay = packet.isOneWay;
            MessageContext msgContext = new MessageContextImpl(packet);
            SOAPMessageContext context =  new SOAPMessageContextImpl(binding,packet,msgContext);
            boolean handlerResult = false;
            // Call handlers on Request
            try {
                if(!soapHandlers.isEmpty()) {
                    if(cousinPipe == null) {
                        //CLIENT-SIDE
                        processor.callHandlersRequest(Direction.OUTBOUND,context,!isOneWay);
                    } else {
                        //SERVER-SIDE
                        processor.callHandlersRequest(Direction.INBOUND,context,!isOneWay);
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
                //TODO: return packet
                return packet;
            }
            
            // Call next Pipe.process() on msg
            Packet reply = next.process(packet);
            
            isOneWay = reply.isOneWay;
            //TODO: For now create again
            msgContext = new MessageContextImpl(packet);
            context =  new SOAPMessageContextImpl(binding,packet,msgContext);
            // Call handlers on Response
            if(cousinPipe == null) {
                //CLIENT-SIDE
                processor.callHandlersResponse(Direction.INBOUND,context,!isOneWay);
            } else {
                //SERVER-SIDE
                processor.callHandlersResponse(Direction.OUTBOUND,context,!isOneWay);
            }
            
            return reply;
        } finally {
            close();
        }
    }

    /**
     * Close SOAPHandlers first and then LogicalHandlers
     */
    public void close(){
        closeSOAPHandlers();
        if(cousinPipe != null){
            // Close LogicalHandlers
            cousinPipe.close();            
        }        
    }
    
    //TODO:
    private void closeSOAPHandlers(){
        
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
