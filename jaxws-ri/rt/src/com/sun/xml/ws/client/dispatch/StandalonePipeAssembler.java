/*
 * Copyright (c) 2006 Your Corporation. All Rights Reserved.
 */
package com.sun.xml.ws.client.dispatch;

import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.WSService;
import com.sun.xml.ws.api.EndpointAddress;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.PipelineAssembler;
import com.sun.xml.ws.api.pipe.TransportPipeFactory;
import com.sun.xml.ws.api.server.WSEndpoint;
import com.sun.xml.ws.binding.soap.SOAPBindingImpl;
import com.sun.xml.ws.handler.HandlerPipe;
import com.sun.xml.ws.handler.LogicalHandlerPipe;
import com.sun.xml.ws.handler.SOAPHandlerPipe;
import com.sun.xml.ws.util.pipe.DumpPipe;
import com.sun.istack.NotNull;
import com.sun.istack.Nullable;

public class StandalonePipeAssembler implements PipelineAssembler {
    public Pipe createClient(EndpointAddress address, WSDLPort wsdlModel, WSService service, WSBinding binding) {
        Pipe head = createTransport(address,wsdlModel,service,binding);
        
        if(dump)
            // for debugging inject a dump pipe. this is left in the production code,
            // as it would be very handy for a trouble-shooting at the production site.
            head = new DumpPipe(System.out,head);
        
        boolean isClient = true;
        HandlerPipe soapHandlerPipe = null;
        //XML/HTTP Binding can have only LogicalHandlerPipe
        if(binding.getSOAPVersion() != null) {
            soapHandlerPipe = new SOAPHandlerPipe(binding, head, isClient);
            head = soapHandlerPipe;
        }
        
        //Someother pipes like JAX-WSA Pipe can come in between LogicalHandlerPipe and
        //SOAPHandlerPipe here.
        
        HandlerPipe logicalHandlerPipe = new LogicalHandlerPipe(binding, head, soapHandlerPipe, isClient);
        head = logicalHandlerPipe;
        
        return head;
    }

    /**
     * Creates a transport pipe (for client), which becomes the terminal pipe.
     */
    protected Pipe createTransport(@NotNull EndpointAddress address, @Nullable WSDLPort wsdlModel, @NotNull WSService service, @NotNull WSBinding binding) {
        return TransportPipeFactory.create(
            Thread.currentThread().getContextClassLoader(),
            address, wsdlModel, service, binding);
    }

    /**
     * On Server-side, HandlerChains cannot be changed after it is deployed.
     * During assembling the Pipelines, we can decide if we really need a 
     * SOAPHandlerPipe and LogicalHandlerPipe for a particular Endpoint.
     */
    public Pipe createServer(WSDLPort wsdlModel, WSEndpoint endpoint, Pipe terminal) {
        WSBinding binding = endpoint.getBinding();
        if((binding.getHandlerChain() != null) &&
                !binding.getHandlerChain().isEmpty()) {
            boolean isClient = false;
            HandlerPipe logicalHandlerPipe = null;
            if(binding.getSOAPVersion() != null) {
                if(!((SOAPBindingImpl)binding).getLogicalHandlerChain().isEmpty()) {
                    logicalHandlerPipe = new LogicalHandlerPipe(binding, terminal, isClient);
                    terminal = logicalHandlerPipe;
                }
            } else {
                //XML/HTTP Binding can have only LogicalHandlers
                logicalHandlerPipe = new LogicalHandlerPipe(binding, terminal, isClient);
                terminal = logicalHandlerPipe;
            }

            //Someother pipes like JAX-WSA Pipe can come in between LogicalHandlerPipe and 
            //SOAPHandlerPipe here.

            if(binding.getSOAPVersion() != null) {
                if(!((SOAPBindingImpl)binding).getSOAPHandlerChain().isEmpty()) {
                    HandlerPipe soapHandlerPipe;
                    soapHandlerPipe= new SOAPHandlerPipe(binding,terminal, logicalHandlerPipe, isClient);
                    terminal = soapHandlerPipe;
                }
            }
        }
        return terminal;
    }

    /**
     * Are we going to dump the message to System.out?
     */
    private static final boolean dump;

    static {
        boolean b = false;
        try {
            b = Boolean.getBoolean(StandalonePipeAssembler.class.getName()+".dump");            
        } catch (Throwable t) {
            // treat it as false
        }
        dump = b;
    }
}
