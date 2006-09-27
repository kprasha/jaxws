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
import com.sun.xml.ws.api.pipe.TubeCloner;
import com.sun.xml.ws.api.pipe.NextAction;
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
    /**
     * @deprecated
     * TODO: remove after a little more of the runtime supports to Fiber
     */
    Pipe next;
    // TODO: For closing in Exceptions this is needed
    // This is used for creating MessageContext in #close
    Packet packet;

    public HandlerPipe(Pipe next, WSDLPort port) {
        super(PipeAdapter.adapt(next));
        this.port = port;
        this.next = next;
    }

    public HandlerPipe(Pipe next, HandlerPipe cousinPipe) {
        super(PipeAdapter.adapt(next));
        this.cousinPipe = cousinPipe;
        if(cousinPipe != null) {
            this.port = cousinPipe.port;
        } else {
            this.port = null;
        }
        this.next = next;
    }

    /**
     * Copy constructor for {@link Pipe#copy(PipeCloner)}.
     */
    protected HandlerPipe(HandlerPipe that, TubeCloner cloner) {
        super(that,cloner);
        if(that.cousinPipe != null) {
            this.cousinPipe = cloner.copy(that.cousinPipe);
        }
        this.port = that.port;
        this.next = ((PipeCloner)cloner).copy(that.next);
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
                // Clean up the exchange for next invocation.
                exchange = null;
                close(context.getMessageContext());
                requestProcessingSucessful = false;
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
            // Clean up the exchange for next invocation.
            exchange = null;
            close(context.getMessageContext());
            requestProcessingSucessful = false;
        }
        //Update Packet with user modifications
        context.updatePacket();

        return doReturnWith(packet);

    }

    @Override
    public NextAction processException(Throwable t) {
        MessageUpdatableContext context = getContext(packet);
        // Clean up the exchange for next invocation.
        exchange = null;
        close(context.getMessageContext());
        requestProcessingSucessful = false;
        //Update Packet with user modifications
        context.updatePacket();

        return doThrow(t);
    }

    public void preDestroy() {
        //TODO Call predestroy on all handlers.
    }

    public final Packet process( Packet packet) {
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
