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
 * SOAPHandlerPipe.java
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
import com.sun.xml.ws.client.HandlerConfiguration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;

/**
 * TODO: Kohsuke put a comment that packet.isOneWay may be null
 * @author WS Development Team
 */
public class SOAPHandlerPipe extends HandlerPipe {

    private WSBinding binding;
    private List<SOAPHandler> soapHandlers;
    private Set<String> roles;

    /** Creates a new instance of SOAPHandlerPipe */
    public SOAPHandlerPipe(WSBinding binding, WSDLPort port, Pipe next, boolean isClient) {
        super(next,port,isClient);
        if(binding.getSOAPVersion() != null) {
            // SOAPHandlerPipe should n't be used for bindings other than SOAP.
            // TODO: throw Exception
        }
        this.binding = binding;
    }

    // Handle to LogicalHandlerPipe means its used on SERVER-SIDE
    /**
     * This constructor is used on client-side where, LogicalHandlerPipe is created
     * first and then a SOAPHandlerPipe is created with a handler to that
     * LogicalHandlerPipe.
     * With this handle, SOAPHandlerPipe can call LogicalHandlerPipe.closeHandlers()
     */
    public SOAPHandlerPipe(WSBinding binding, Pipe next, HandlerPipe cousinPipe, boolean isClient) {
        super(next,cousinPipe,isClient);
        this.binding = binding;
    }

    /**
     * Copy constructor for {@link com.sun.xml.ws.api.pipe.Pipe#copy(com.sun.xml.ws.api.pipe.PipeCloner)}.
     */
    protected SOAPHandlerPipe(SOAPHandlerPipe that, PipeCloner cloner) {
        super(that,cloner);
        this.binding = that.binding;
    }

    boolean isHandlerChainEmpty() {
        return soapHandlers.isEmpty();
    }

    /**
     * Close SOAPHandlers first and then LogicalHandlers on Client
     * Close LogicalHandlers first and then SOAPHandlers on Server
     */
    public void close(MessageContext msgContext){
        if(!isClient){
            if(cousinPipe != null){
                // Close LogicalHandlerPipe
                cousinPipe.closeCall(msgContext);
            }
            if(processor != null)
                closeSOAPHandlers(msgContext);

        }

    }
    /**
     * This is called from cousinPipe.
     * Close this Pipes's handlers.
     */
    public void closeCall(MessageContext msgContext){
        closeSOAPHandlers(msgContext);
    }

    //TODO:
    private void closeSOAPHandlers(MessageContext msgContext){
        if(processor == null)
            return;
        if(remedyActionTaken) {
            //Close only invoked handlers in the chain
            if(isClient){
                //CLIENT-SIDE
                processor.closeHandlers(msgContext,processor.getIndex(),0);
            } else {
                //SERVER-SIDE
                processor.closeHandlers(msgContext,processor.getIndex(),soapHandlers.size()-1);
            }
        } else {
            //Close all handlers in the chain
            if(isClient){
                //CLIENT-SIDE
                processor.closeHandlers(msgContext,soapHandlers.size()-1,0);
            } else {
                //SERVER-SIDE
                processor.closeHandlers(msgContext,0,soapHandlers.size()-1);
            }
        }
    }

    public Pipe copy(PipeCloner cloner) {
        return new SOAPHandlerPipe(this,cloner);
    }

    void setUpProcessor() {
        // Take a snapshot, User may change chain after invocation, Same chain
        // should be used for the entire MEP
        soapHandlers = new ArrayList<SOAPHandler>();
        HandlerConfiguration handlerConfig = ((BindingImpl)binding).getHandlerConfig();
        soapHandlers.addAll(handlerConfig.getSoapHandlers());
        roles = new HashSet<String>();
        roles.addAll(handlerConfig.getRoles());
        processor = new SOAPHandlerProcessor(this,binding,soapHandlers, isClient);
    }

    MessageUpdatableContext getContext(Packet packet){
        SOAPMessageContextImpl context =  new SOAPMessageContextImpl(binding,packet);
        context.setRoles(roles);
        return context;
    }

}
