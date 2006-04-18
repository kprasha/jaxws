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

/*
 * LogicalHandlerPipe.java
 *
 * Created on February 9, 2006, 11:12 PM
 *
 *
 */

package com.sun.xml.ws.handler;

import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
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
    private List<LogicalHandler> logicalHandlers;    
    /** Creates a new instance of LogicalHandlerPipe */
    public LogicalHandlerPipe(WSBinding binding, WSDLPort port, Pipe next, boolean isClient) {
        super(next, port, isClient);
        this.binding = binding;
    }
    
    
    /**
     * This constructor is used on client-side where, SOAPHandlerPipe is created
     * first and then a LogicalHandlerPipe is created with a handler to that
     * SOAPHandlerPipe.
     * With this handle, LogicalHandlerPipe can call
     * SOAPHandlerPipe.closeHandlers()
     */
    public LogicalHandlerPipe(WSBinding binding, Pipe next, HandlerPipe cousinPipe, boolean isClient) {
        super(next,cousinPipe, isClient);
        this.binding = binding;
    }
    
    /**
     * Copy constructor for {@link com.sun.xml.ws.api.pipe.Pipe#copy(com.sun.xml.ws.api.pipe.PipeCloner)}.
     */
    
    private LogicalHandlerPipe(LogicalHandlerPipe that, PipeCloner cloner) {
        super(that,cloner);
        this.binding = that.binding;
    }
    
    boolean isHandlerChainEmpty() {
        return logicalHandlers.isEmpty();
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
    
    public Pipe copy(PipeCloner cloner) {
        return new LogicalHandlerPipe(this,cloner);
    }
    
    void setUpProcessor() {
        // Take a snapshot, User may change chain after invocation, Same chain
        // should be used for the entire MEP
        logicalHandlers = new ArrayList<LogicalHandler>();
        logicalHandlers.addAll(((BindingImpl)binding).getHandlerConfig().getLogicalHandlers());
        if(binding.getSOAPVersion() == null) {
            processor = new XMLHandlerProcessor(this, binding,
                    logicalHandlers,isClient);
        } else {
            processor = new SOAPHandlerProcessor(this, binding,
                    logicalHandlers,isClient);
        }            
    }
    
    
     MessageUpdatableContext getContext(Packet packet){
        return new LogicalMessageContextImpl(binding,packet);                
    }
    
}
