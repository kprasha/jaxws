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
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.encoding.soap.SOAP12Constants;
import com.sun.xml.ws.encoding.soap.SOAPConstants;
import com.sun.xml.ws.encoding.soap.streaming.SOAP12NamespaceConstants;
import com.sun.xml.ws.handler.HandlerContext;
import com.sun.xml.ws.handler.HandlerException;
import com.sun.xml.ws.handler.XMLHandlerContext;
import com.sun.xml.ws.handler.MessageContextUtil;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.LogicalMessage;
import javax.xml.ws.ProtocolException;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.LogicalHandler;
import javax.xml.ws.handler.LogicalMessageContext;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import javax.xml.ws.soap.SOAPFaultException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * @author WS Development Team
 */
abstract class HandlerProcessor<C extends MessageContext> {
    
    public static final String IGNORE_FAULT_PROPERTY =
            "ignore fault in message";
    public static final String HANDLE_FAULT_PROPERTY =
            "handle fault on message";
    public static final String HANDLE_FALSE_PROPERTY =
            "handle false on message";
    protected boolean isClient;
    protected static final Logger logger = Logger.getLogger(
            com.sun.xml.ws.util.Constants.LoggingDomain + ".handler");
    
    // need request or response for Handle interface
    public enum RequestOrResponse { REQUEST, RESPONSE }
    public enum Direction { OUTBOUND, INBOUND }
    private Set<QName> understoodHeaders;
    private List<Handler> handlers; // may be logical/soap mixed
    
    private WSBinding binding;
    private int index=0;
    
    /**
     * The handlers that are passed in will be sorted into
     * logical and soap handlers. During this sorting, the
     * understood headers are also obtained from any soap
     * handlers.
     *
     * @param chain A list of handler objects, which can
     * be protocol or logical handlers.
     */
    public HandlerProcessor(WSBinding binding, List<Handler> chain, boolean isClient) {
        if (chain == null) { // should only happen in testing
            chain = new ArrayList<Handler>();
        }
        handlers = chain;
        this.binding = binding;
        this.isClient = isClient;
    }
    
    /**
     * Gives index of the handler in the chain to know what handlers in the chain
     * are invoked
     */
    protected int getIndex() {
        return index;
    }
    /**
     * This is called when a handler returns false or throws a RuntimeException
     */
    private void setIndex(int i) {
        //TODO: If its already set, don't modify it
        index = i;
    }
    /**
     * This list may be different than the chain that is passed
     * in since the logical and protocol handlers must be separated.
     *
     * @return The list of handlers, sorted by logical and then protocol.
     */
    public List<Handler> getHandlerChain() {
        return handlers;
    }
    
    public boolean hasHandlers() {
        return (handlers.size() != 0);
    }
    
    /**
     * TODO: Just putting thoughts,
     * Current contract: This is Called during Request Processing.
     * return true, if all handlers in the chain return true
     *            Current Pipe can call nextPipe.process();
     * return false, One of the handlers has returned false or thrown a
     *            RuntimeException. Remedy Actions taken:
     *         1) In this case, The processor will setIndex()to track what 
     *            handlers are invoked until that point.
     *         2) Previously invoked handlers are again invoked (handleMessage()
     *            or handleFault()) to take remedy action. 
     *            CurrentPipe should NOT call nextPipe.process()
     *            While closing handlers, check getIndex() to get the invoked 
     *            handlers.
     * TODO: Index may be reset during remedy action, needs fix
     * throw RuntimeException, this happens when a RuntimeException occurs during
     *            handleMessage during Request processing or 
     *            during remedy action 2)
     *            CurrentPipe should NOT call nextPipe.process() and throw the 
     *            exception to the previous Pipe
     *            While closing handlers, check getIndex() to get the invoked 
     *            handlers.
     */
    public boolean callHandlersRequest(Direction direction,
            C context,
            boolean responseExpected) {
        setDirection(direction,context);
        boolean result = true;
        // call handlers
        try {
            if (direction == Direction.OUTBOUND) {
                result = callHandleMessage(context,0,handlers.size()-1);
            } else {
                result = callHandleMessage(context,handlers.size()-1,0);
            }
        } catch (ProtocolException pe) {
            logger.log(Level.FINER, "exception in handler chain", pe);
            if (responseExpected) {
                //insert fault message if its not a fault message
                insertFaultMessage(context, (ProtocolException) pe);                
                // reverse direction
                reverseDirection(direction,context);
                //Put handleFault in MessageContext
                addHandleFaultProperty(context);
                // call handle fault                
                if(direction == Direction.OUTBOUND) {
                    callHandleFault(context, getIndex()-1, 0);
                } else {
                    callHandleFault(context,getIndex()+1,handlers.size()-1);
                }
            }
            return false;
        } catch (RuntimeException re) {
            logger.log(Level.FINER, "exception in handler chain", re);
            throw re;
        }
        
        if(!result) {
            if (responseExpected) {
                // reverse direction
                reverseDirection(direction,context);
                // call handle message
                if(direction == Direction.OUTBOUND) {
                    callHandleMessageReverse(context, getIndex()-1, 0);
                } else {
                    callHandleMessageReverse(context,getIndex()+1,handlers.size()-1);
                }
            }
            return false;
        }
        
        return result;
    }
    
    
    /**
     * TODO: Just putting thoughts,
     * Current contract: This is Called during Response Processing.
     * Runs all handlers until handle returns false or throws a RuntimeException
     *            CurrentPipe should close all the handlers in the chain.  
     * throw RuntimeException, this happens when a RuntimeException occurs during
     *            normal Response processing or remedy action 2) taken
     *            during callHandlersRequest().
     *            CurrentPipe should close all the handlers in the chain.  
     * TODO: there might be a problem with Index tracking in some cases.
     */
    public void callHandlersResponse(Direction direction,
            C context) {
        setDirection(direction,context);
        boolean callHandleFalse = (context.get(HANDLE_FALSE_PROPERTY) == null)? 
            false:(Boolean) context.get(HANDLE_FALSE_PROPERTY);
        if(callHandleFalse){
            // Cousin HandlerPipe returned false during Response processing.
            // Don't call handlers.
            return;
        }
        
        boolean callHandleFault = (context.get(HANDLE_FAULT_PROPERTY) == null)? 
            false:(Boolean) context.get(HANDLE_FAULT_PROPERTY);
        try {
            if(callHandleFault) {
                // call handleFault on handlers
                if(direction == Direction.OUTBOUND) {
                    callHandleFault(context,0,handlers.size()-1);
                } else {
                    callHandleFault(context,handlers.size()-1,0);
                }
            } else {
                // call handleMessage on handlers                
                if (direction == Direction.OUTBOUND) {
                    callHandleMessageReverse(context,0,handlers.size()-1);
                } else {
                    callHandleMessageReverse(context,handlers.size()-1,0);
                }
            }
        } catch (RuntimeException re) {
            logger.log(Level.FINER, "exception in handler chain", re);
            throw re;
        }        
    }
    
    /**
     * Reverses the Message Direction.
     * MessageContext.MESSAGE_OUTBOUND_PROPERTY is changed.
     */
    public void reverseDirection(Direction origDirection, C context){
        if(origDirection == Direction.OUTBOUND){
            context.put(MessageContext.MESSAGE_OUTBOUND_PROPERTY, false);
        } else {
            context.put(MessageContext.MESSAGE_OUTBOUND_PROPERTY, true);
        }
    }
    
    /**
     * Sets the Message Direction.
     * MessageContext.MESSAGE_OUTBOUND_PROPERTY is changed.
     */
    public void setDirection(Direction direction, C context){
        if(direction == Direction.OUTBOUND){
            context.put(MessageContext.MESSAGE_OUTBOUND_PROPERTY, true);
        } else {
            context.put(MessageContext.MESSAGE_OUTBOUND_PROPERTY, false);
        }
    }
    
    /**
     * When this property is set HandlerPipes can call handleFault() on the 
     * message
     */
    private void addHandleFaultProperty(C context) {
        context.put(HANDLE_FAULT_PROPERTY, Boolean.TRUE);
    }
    
    /**
     * When this property is set HandlerPipes will not call 
     * handleMessage() during Response processing.
     */
    private void addHandleFalseProperty(C context) {
        context.put(HANDLE_FALSE_PROPERTY, Boolean.TRUE);
    }
    
    /**
     * TODO: Do we need this? 
     * <p>The expectation of the rest of the code is that,
     * if a ProtocolException is thrown from the handler chain,
     * the message contents reflect the protocol exception.
     * However, if a new ProtocolException is thrown from
     * the handleFault method, then the fault should be
     * ignored and the new exception should be dispatched.
     *
     * <p>This method simply sets a property that is checked
     * by the client and server code when a ProtocolException
     * is caught. The property can be checked with
     * {@link MessageContextUtil#ignoreFaultInMessage}
     */
    private void addIgnoreFaultProperty(C context) {
        context.put(IGNORE_FAULT_PROPERTY, Boolean.TRUE);
    }

    /**
     * When a ProtocolException is thrown, this is called.
     * If it's XML/HTTP Binding, clear the the message
     * If its SOAP/HTTP Binding, put right SOAP Fault version
     */
    abstract void insertFaultMessage(C context,
            ProtocolException exception);
    
    /*
     * Calls handleMessage on the handlers. Indices are
     * inclusive. Exceptions get passed up the chain, and an
     * exception or return of 'false' ends processing.
     */
    private boolean callHandleMessage(C context, int start, int end) {
        /* Do we need this check?
        if (handlers.isEmpty() ||
                start == -1 ||
                start == handlers.size()) {
            return false;
        }
         */
        int i = start;
        try {
            if (start > end) {
                while(i >= end) {
                    if(handlers.get(i).handleMessage(context) == false) {
                        setIndex(i);
                        return false;
                    }
                    i--;
                }
            } else {
                while(i <= end) {
                    if(handlers.get(i).handleMessage(context) == false) {
                        setIndex(i);
                        return false;
                    }
                    i++;
                }
            }
        } catch(RuntimeException e) {
            setIndex(i);
            throw e;
        }
        return true;
    }
    
    /*
     * Calls handleMessage on the handlers. Indices are
     * inclusive. Exceptions get passed up the chain, and an
     * exception (or) 
     * return of 'false' calls addHandleFalseProperty(context) and 
     * ends processing.
     * setIndex() is not called.
     *
     */
    private boolean callHandleMessageReverse(C context, int start, int end) {
        /* Do we need this check?
        if (handlers.isEmpty() ||
                start == -1 ||
                start == handlers.size()) {
            return false;
        }
         */
        int i = start;
        
        if (start > end) {
            while(i >= end) {
                if(handlers.get(i).handleMessage(context) == false) {
                    addHandleFalseProperty(context);
                    return false;
                }
                i--;
            }
        } else {
            while(i <= end) {
                if(handlers.get(i).handleMessage(context) == false) {
                    addHandleFalseProperty(context);
                    return false;
                }
                i++;
            }
        }        
        return true;
    }
    
    /*
     * Calls handleFault on the handlers. Indices are
     * inclusive. Exceptions get passed up the chain, and an
     * exception or return of 'false' ends processing.
     */
    
    private boolean callHandleFault(C context, int start, int end) {
        
        if (handlers.isEmpty()) {
            return true;
        }
        int i = start;
        if (start > end) {
            try {
                while (i >= end) {
                    if (handlers.get(i).handleFault(context) == false) {
                        return false;
                    }
                    i--;
                }
            } catch (RuntimeException re) {
                logger.log(Level.FINER,
                        "exception in handler chain", re);
                throw re;
            }
        } else {
            try {
                while (i <= end) {
                    if (handlers.get(i).handleFault(context) == false) {
                        return false;
                    }
                    i++;
                }
            } catch (RuntimeException re) {
                logger.log(Level.FINER,
                        "exception in handler chain", re);
                throw re;
            }
        }
        return true;
    }
    
    /**
     * Calls close on the handlers from the starting
     * index through the ending index (inclusive). Made indices
     * inclusive to allow both directions more easily.
     */
    protected void closeHandlers(MessageContext context, int start, int end) {
        
        if (handlers.isEmpty()) {
            return;
        }
        if (start > end) {
            for (int i=start; i>=end; i--) {
                try {
                    handlers.get(i).close(context);
                } catch (RuntimeException re) {
                    logger.log(Level.INFO,
                            "Exception ignored during close", re);
                }
            }
        } else {
            for (int i=start; i<=end; i++) {
                try {
                    handlers.get(i).close(context);
                } catch (RuntimeException re) {
                    logger.log(Level.INFO,
                            "Exception ignored during close", re);
                }
            }
        }
    }
    
    
    
    /**
     * Used to hold the context objects that are used to get
     * and set the current message.
     *
     */
    /*
    static class ContextHolder {
        
        boolean logicalOnly;
        WSBinding binding;
        Packet packet;
        MessageContext cxt;
        // smc and lmc are mutually exclusive, one of them should be null anytime
        SOAPMessageContextImpl smc;
        LogicalMessageContextImpl lmc;
        ContextHolder(WSBinding binding, Packet msg) {
            logicalOnly = isLogicalOnly(binding);
            this.binding = binding;
            this.packet = msg;
            this.cxt = new MessageContextImpl(msg);
        }
        
        
        // This method determines whether to process ProtocolHandlers or not
         
        public boolean isLogicalOnly(WSBinding binding) {
            if(binding.getSOAPVersion()!=null) {
                return false;
            }
            return true;
        }
        
        LogicalMessageContext getLMC() {
            if(lmc == null) {
                if(smc != null) {
                    smc.updatePacket();
                    smc = null;
                }
                lmc = new LogicalMessageContextImpl(binding, packet, cxt);
                
            }
            return lmc;
        }
        
        SOAPMessageContext getSMC() {
            if(smc == null) {
                if(lmc != null) {
                    lmc.updatePacket();
                    lmc = null;
                }
                smc = (logicalOnly ? null : new SOAPMessageContextImpl(binding, packet, cxt));
            }
            return smc;
        }
        
        MessageContext getMC() {
            return cxt;
        }
        //TODO:
        Packet getUpdatedPacket() {
            //TODO: Need to build a Message and set properties from MessageContext
            if(lmc != null) {
                lmc.updatePacket();
            } else if(smc != null) {
                smc.updatePacket();
            }
            return packet;
        }
        
        
    }
    */
    /* TRI-STATE for handler result
    public enum HandleResult { TRUE, FALSE, EXCEPTION };
    
    class HandlerResult {        
        boolean result = true;
        int index = 0;
        RuntimeException ex;
        HandleResult internalResult;
        
        public HandlerResult(boolean result){
            this.result = result;
            if(result)
                internalResult = HandleResult.TRUE;
            else
                internalResult = HandleResult.FALSE;
        }
        public HandlerResult(boolean result, int index){
            if(result) {
                internalResult = HandleResult.TRUE;
            } else {
                internalResult = HandleResult.FALSE;
            }
            this.index = index;
            this.result = result;
        }
        
        public HandlerResult(RuntimeException ex) {
            internalResult = HandleResult.EXCEPTION;
            this.ex = ex;
        }
        
        public HandleResult getHandlerResult() {
            return internalResult;
        }
        
        boolean getResult(){
            return result;
        }
        
        RuntimeException getException() {
            return ex;
        }
        
        int getIndex(){
            return index;
        }
    }
    */
}
