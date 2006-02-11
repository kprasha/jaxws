/*
 * LogicalHandlerProcessor.java
 *
 * Created on February 8, 2006, 5:40 PM
 * 
 */

package com.sun.xml.ws.sandbox.handler;

import com.sun.xml.ws.api.WSBinding;
import java.util.List;
import javax.xml.ws.LogicalMessage;
import javax.xml.ws.ProtocolException;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.LogicalMessageContext;

/**
 *
 * @author WS Development Team
 */
public class LogicalHandlerProcessor<C extends LogicalMessageContext> extends HandlerProcessor<C> {
    
    /**
     * Creates a new instance of LogicalHandlerProcessor
     */
    public LogicalHandlerProcessor(WSBinding binding, List<Handler> chain) {
    super(binding,chain);
    }
    
    void insertFaultMessage(C context,
            ProtocolException exception) {
        
        if (context == null) {
            // non-soap case
            LogicalMessage msg = context.getMessage();
            if (msg != null) {
                msg.setPayload(null);
            }
            return;
        }
    }
}
