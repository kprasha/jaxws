/*
 * LogicalHandlerProcessor.java
 *
 * Created on February 8, 2006, 5:40 PM
 * 
 */

package com.sun.xml.ws.handler;

import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.message.Messages;
import com.sun.xml.ws.sandbox.message.impl.EmptyMessageImpl;
import java.util.List;
import javax.xml.ws.LogicalMessage;
import javax.xml.ws.ProtocolException;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.LogicalMessageContext;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.http.HTTPException;

/**
 * This is used only for XML/HTTP binding
 * @author WS Development Team
 */
public class XMLHandlerProcessor<C extends MessageUpdatableContext> extends HandlerProcessor<C> {
    
    /**
     * Creates a new instance of LogicalHandlerProcessor
     */
    public XMLHandlerProcessor(WSBinding binding, List<Handler> chain, boolean isClient) {
        super(binding,chain, isClient);
    }
    
    /*
     * TODO: This is valid only for XML/HTTP binding
     * Empty the XML message
     */
    void insertFaultMessage(C context,
            ProtocolException exception) {
        if(exception instanceof HTTPException) {
            context.put(MessageContext.HTTP_RESPONSE_CODE,((HTTPException)exception).getStatusCode());
        }
        if (context != null) {
            // non-soap case
            context.setPacketMessage(Messages.createEmpty(binding.getSOAPVersion()));            
        }        
    }
}
