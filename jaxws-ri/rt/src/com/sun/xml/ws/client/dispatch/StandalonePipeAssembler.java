/*
 * Copyright (c) 2006 Your Corporation. All Rights Reserved.
 */
package com.sun.xml.ws.client.dispatch;

import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.server.WSEndpoint;
import com.sun.xml.ws.api.WSService;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.PipelineAssembler;
import com.sun.xml.ws.handler.HandlerPipe;
import com.sun.xml.ws.sandbox.handler.LogicalHandlerPipe;
import com.sun.xml.ws.sandbox.handler.SOAPHandlerPipe;
import com.sun.xml.ws.transport.http.client.HttpTransportPipe;

public class StandalonePipeAssembler implements PipelineAssembler {
    public Pipe createClient(WSDLPort wsdlModel, WSService service, WSBinding binding) {
        Pipe head = createTransport(wsdlModel,service,binding);
        if(!binding.getHandlerChain().isEmpty()) {
            boolean isClient = true;            
            if(binding.getSOAPVersion() != null) {
                HandlerPipe soapHandlerPipe = new SOAPHandlerPipe(binding, head, isClient);
                HandlerPipe logicalHandlerPipe = new LogicalHandlerPipe(binding, soapHandlerPipe, soapHandlerPipe, isClient);
                head = logicalHandlerPipe;
            } else {
                //XML/HTTP Binding can have only LogicalHandlers
                HandlerPipe logicalHandlerPipe = new LogicalHandlerPipe(binding, head, isClient);
                head = logicalHandlerPipe;
            }    
            
        }         
        return head;
    }

    protected Pipe createTransport(WSDLPort wsdlModel, WSService service, WSBinding binding) {
        return new HttpTransportPipe(binding);
    }

    public Pipe createServer(WSDLPort wsdlModel, WSEndpoint endpoint, Pipe terminal) {
        WSBinding binding = endpoint.getBinding();
        if(!binding.getHandlerChain().isEmpty()) {
            boolean isClient = false;
            if(binding.getSOAPVersion() != null) {
                HandlerPipe logicalHandlerPipe = new LogicalHandlerPipe(binding, terminal, isClient);
                HandlerPipe soapHandlerPipe = new SOAPHandlerPipe(binding, logicalHandlerPipe, logicalHandlerPipe, isClient);
                terminal = soapHandlerPipe;
            } else {
                //XML/HTTP Binding can have only LogicalHandlers
                HandlerPipe logicalHandlerPipe = new LogicalHandlerPipe(binding, terminal, isClient);
                terminal = logicalHandlerPipe;
            }            
        }
        return terminal;
    }
}
