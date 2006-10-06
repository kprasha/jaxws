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
import com.sun.xml.ws.api.pipe.*;
import com.sun.xml.ws.api.pipe.helper.AbstractFilterPipeImpl;
import com.sun.xml.ws.api.pipe.helper.AbstractFilterTubeImpl;
import com.sun.xml.ws.api.pipe.helper.PipeAdapter;
import com.sun.xml.ws.handler.HandlerProcessor.Direction;

import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.MessageContext;

/**
 * @author WS Development team
 */

public abstract class HandlerPipe extends AbstractFilterTubeImpl {
    
    /**
     * handle hold reference to other Pipe for inter-pipe communication
     */
    HandlerPipe cousinPipe;
    HandlerProcessor processor;
    boolean remedyActionTaken = false;
    private final @Nullable WSDLPort port;
    // flag used to decide whether to call close on cousinPipe
    boolean requestProcessingSucessful = false;

    // TODO: For closing in Exceptions this is needed
    // This is used for creating MessageContext in #close
    Packet packet;

    public HandlerPipe(Tube next, WSDLPort port) {
        super(next);
        this.port = port;
    }

    /*
    public HandlerPipe(Pipe next, WSDLPort port) {
        this(PipeAdapter.adapt(next), port);
    }
    */

    public HandlerPipe(Tube next, HandlerPipe cousinPipe) {
        super(next);
        this.cousinPipe = cousinPipe;
        if(cousinPipe != null) {
            this.port = cousinPipe.port;
        } else {
            this.port = null;
        }
    }

    /*
    public HandlerPipe(Pipe next, HandlerPipe cousinPipe) {
        this(PipeAdapter.adapt(next), cousinPipe);
    }
    */

    /**
     * Copy constructor for {@link Pipe#copy(PipeCloner)}.
     */
    protected HandlerPipe(HandlerPipe that, TubeCloner cloner) {
        super(that,cloner);
        if(that.cousinPipe != null) {
            this.cousinPipe = cloner.copy(that.cousinPipe);
        }
        this.port = that.port;        
    }

    @Override
    public NextAction processRequest(Packet request) {
        this.packet = request;
        setupExchange();
        // This check is done to cover handler returning false in Oneway request
        if (isHandleFalse()) {
            // Cousin HandlerTube returned false during Oneway Request processing.
            // Don't call handlers and dispatch the message.
            remedyActionTaken = true;
            return doInvoke(super.next, packet);
        }

        // This is done here instead of the constructor, since User can change
        // the roles and handlerchain after a stub/proxy is created.
        setUpProcessor();

        MessageUpdatableContext context = getContext(packet);
        try {
            boolean isOneWay = checkOneWay(packet);
            if (!isHandlerChainEmpty()) {
                // Call handlers on Request
                boolean handlerResult = callHandlersOnRequest(context, isOneWay);
                //Update Packet with user modifications
                context.updatePacket();
                // the only case where no message is sent
                if (!isOneWay && !handlerResult) {
                    return doReturnWith(packet);
                }
            }
            requestProcessingSucessful = true;
            // Call next Pipe.process() on msg
            return doInvoke(super.next, packet);
        } finally {
            if(!requestProcessingSucessful) {
                cleanUpState(context.getMessageContext());
            }
        }

    }

    @Override
    public NextAction processResponse(Packet response) {
        this.packet = response;
        MessageUpdatableContext context = getContext(packet);
        try {
            if (isHandleFalse() || (packet.getMessage() == null)) {
                // Cousin HandlerTube returned false during Response processing.
                // or it is oneway request
                // or handler chain is empty
                // Don't call handlers.
                return doReturnWith(packet);
            }
            boolean isFault = isHandleFault(packet);
            if (!isHandlerChainEmpty()) {
                // Call handlers on Response
                callHandlersOnResponse(context, isFault);
            }
        } finally {
            cleanUpState(context.getMessageContext());
        }
        //Update Packet with user modifications
        context.updatePacket();

        return doReturnWith(packet);

    }

    @Override
    public NextAction processException(Throwable t) {
        try {
            return doThrow(t);
        } finally {
            MessageUpdatableContext context = getContext(packet);
            cleanUpState(context.getMessageContext());
            /* TODO revisit: commented this out as the modified packet is no longer used
                    In future if the message is propagated even when an exception
                    occurs, then uncomment context.updatePacket();
            */
            //Update Packet with user modifications
            //context.updatePacket();


        }
    }

    /**
     * Calls close on previously invoked handlers.
     * Also, Cleans up any state left over in the Tube instance from the current
     * invocation, as Tube instances can be reused after the completion of MEP.
     *
     */
    private void cleanUpState(MessageContext mc) {
        close(mc);
        // Clean up the exchange for next invocation.
        exchange = null;
        requestProcessingSucessful = false;
    }

    /* public final Packet process( Packet packet) {
        setupExchange();
        // This check is done to cover handler returning false in Oneway request
        if(isHandleFalse()){
            // Cousin HandlerPipe returned false during Oneway Request processing.
            // Don't call handlers and dispatch the message.
            remedyActionTaken = true;
            return next.process(packet);
        }

        // This is done here instead of the constructor, since User can change
        // the roles and handlerchain after a stub/proxy is created.
        setUpProcessor();

        MessageUpdatableContext context = getContext(packet);
        Packet reply;
        try {
            boolean isOneWay = checkOneWay(packet);
            if(!isHandlerChainEmpty()) {
                // Call handlers on Request
                boolean handlerResult = callHandlersOnRequest(context, isOneWay);
                //Update Packet with user modifications
                context.updatePacket();
                // the only case where no message is sent
                if (!isOneWay && !handlerResult) {
                    return packet;
                }
            }
            requestProcessingSucessful = true;
            // Call next Pipe.process() on msg
            reply = next.process(packet);
            context =  getContext(reply);
            if (isHandleFalse() || (reply.getMessage() == null)) {
                // Cousin HandlerPipe returned false during Response processing.
                // or it is oneway request
                // or handler chain is empty
                // Don't call handlers.
                return reply;
            }
            boolean isFault = isHandleFault(reply);
            if( !isHandlerChainEmpty()) {
                // Call handlers on Response
                callHandlersOnResponse(context,isFault);
            }
        } finally {
            // Clean up the exchange for next invocation.
            exchange = null;
            close(context.getMessageContext());
            requestProcessingSucessful = false;
        }
        //Update Packet with user modifications
        context.updatePacket();

        return reply;
    }
    */
    abstract void callHandlersOnResponse(MessageUpdatableContext context, boolean handleFault);

    abstract boolean callHandlersOnRequest(MessageUpdatableContext context, boolean oneWay);

    private boolean checkOneWay(Packet packet) {
        if (port != null) {
            /* we can determine this value from WSDL */
            return packet.getMessage().isOneWay(port);
        } else {
            /*
              otherwise use this value as an approximation, since this carries
              the appliation's intention --- whether it was invokeOneway vs invoke,etc.
             */
            return (packet.expectReply != null && packet.expectReply);
        }
    }

    abstract void setUpProcessor();
    abstract boolean isHandlerChainEmpty();
    abstract MessageUpdatableContext getContext(Packet p);
    
    /**
     * Close SOAPHandlers first and then LogicalHandlers on Client
     * Close LogicalHandlers first and then SOAPHandlers on Server
     */
    protected abstract void close(MessageContext msgContext);
    
    /**
     * This is called from cousinPipe.
     * Close this Pipes's handlers.
     */
    protected abstract void closeCall(MessageContext msgContext);

    private boolean isHandleFault(Packet packet) {
        if (cousinPipe != null) {
            return exchange.isHandleFault();
        } else {
            boolean isFault = packet.getMessage().isFault();
            exchange.setHandleFault(isFault);
            return isFault;
        }
    }

    final void setHandleFault() {
        exchange.setHandleFault(true);
    }

    private boolean isHandleFalse() {
        return exchange.isHandleFalse();
    }

    final void setHandleFalse() {
        exchange.setHandleFalse();
    }

    private void setupExchange() {
        if(exchange == null) {
            exchange = new HandlerPipeExchange();
            if(cousinPipe != null) {
                cousinPipe.exchange = exchange;
            }
        }        
    }
    private HandlerPipeExchange exchange;

    /**
     * This class is used primarily to exchange information or status between
     * LogicalHandlerPipe and SOAPHandlerPipe
     */
    static final class HandlerPipeExchange {
        private boolean handleFalse;
        private boolean handleFault;

        boolean isHandleFault() {
            return handleFault;
        }

        void setHandleFault(boolean isFault) {
            this.handleFault = isFault;
        }

        public boolean isHandleFalse() {
            return handleFalse;
        }

        void setHandleFalse() {
            this.handleFalse = true;
        }
    }
    
}
