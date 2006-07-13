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
package com.sun.xml.ws.handler;

import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.PipeCloner;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.binding.BindingImpl;

import javax.xml.ws.handler.LogicalHandler;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.WebServiceException;
import java.util.List;
import java.util.ArrayList;

/**
 *
 * @author WS Development Team
 */
public class ClientLogicalHandlerPipe extends HandlerPipe {

    private WSBinding binding;
    private List<LogicalHandler> logicalHandlers;

    /**
     * Creates a new instance of LogicalHandlerPipe
     */
    public ClientLogicalHandlerPipe(WSBinding binding, WSDLPort port, Pipe next) {
        super(next, port);
        this.binding = binding;
    }


    /**
     * This constructor is used on client-side where, SOAPHandlerPipe is created
     * first and then a LogicalHandlerPipe is created with a handler to that
     * SOAPHandlerPipe.
     * With this handle, LogicalHandlerPipe can call
     * SOAPHandlerPipe.closeHandlers()
     */
    public ClientLogicalHandlerPipe(WSBinding binding, Pipe next, HandlerPipe cousinPipe) {
        super(next, cousinPipe);
        this.binding = binding;
    }

    /**
     * Copy constructor for {@link com.sun.xml.ws.api.pipe.Pipe#copy(com.sun.xml.ws.api.pipe.PipeCloner)}.
     */

    private ClientLogicalHandlerPipe(ClientLogicalHandlerPipe that, PipeCloner cloner) {
        super(that, cloner);
        this.binding = that.binding;
    }

    boolean isHandlerChainEmpty() {
        return logicalHandlers.isEmpty();
    }

    /**
     * Close SOAPHandlers first and then LogicalHandlers on Client
     * Close LogicalHandlers first and then SOAPHandlers on Server
     */
    public void close(MessageContext msgContext) {
        //assuming cousinPipe is called if requestProcessingSucessful is true
        if (requestProcessingSucessful) {
            //cousinPipe is null in XML/HTTP Binding
            if (cousinPipe != null) {
                // Close SOAPHandlerPipe
                cousinPipe.closeCall(msgContext);
            }
        }
        if (processor != null)
            closeLogicalHandlers(msgContext);

    }

    /**
     * This is called from cousinPipe.
     * Close this Pipes's handlers.
     */
    public void closeCall(MessageContext msgContext) {
        closeLogicalHandlers(msgContext);
    }

    //TODO:
    private void closeLogicalHandlers(MessageContext msgContext) {
        if (processor == null)
            return;
        if (remedyActionTaken) {
            //Close only invoked handlers in the chain

            //CLIENT-SIDE
            processor.closeHandlers(msgContext, processor.getIndex(), 0);
            //reset remedyActionTaken
            remedyActionTaken = false;
        } else {
            //Close all handlers in the chain

            //CLIENT-SIDE
            processor.closeHandlers(msgContext, logicalHandlers.size() - 1, 0);

        }
    }

    public Pipe copy(PipeCloner cloner) {
        return new ClientLogicalHandlerPipe(this, cloner);
    }

    void setUpProcessor() {
        // Take a snapshot, User may change chain after invocation, Same chain
        // should be used for the entire MEP
        logicalHandlers = new ArrayList<LogicalHandler>();
        logicalHandlers.addAll(((BindingImpl) binding).getHandlerConfig().getLogicalHandlers());
        if (binding.getSOAPVersion() == null) {
            processor = new XMLHandlerProcessor(this, binding,
                    logicalHandlers);
        } else {
            processor = new SOAPHandlerProcessor(true, this, binding,
                    logicalHandlers);
        }
    }


    MessageUpdatableContext getContext(Packet packet) {
        return new LogicalMessageContextImpl(binding, packet);
    }

    boolean callHandlersOnRequest(MessageUpdatableContext context, boolean isOneWay) {

        boolean handlerResult;
        try {

            //CLIENT-SIDE
            handlerResult = processor.callHandlersRequest(HandlerProcessor.Direction.OUTBOUND, context, !isOneWay);
        } catch (WebServiceException wse) {
            remedyActionTaken = true;
            //no rewrapping
            throw wse;
        } catch (RuntimeException re) {
            remedyActionTaken = true;

            throw new WebServiceException(re);

        }
        if (!handlerResult) {
            remedyActionTaken = true;
        }
        return handlerResult;
    }

    void callHandlersOnResponse(MessageUpdatableContext context, boolean handleFault) {
        try {

            //CLIENT-SIDE
            processor.callHandlersResponse(HandlerProcessor.Direction.INBOUND, context, handleFault);

        } catch (WebServiceException wse) {
            //no rewrapping
            throw wse;
        } catch (RuntimeException re) {

            throw new WebServiceException(re);

        }
    }
}
