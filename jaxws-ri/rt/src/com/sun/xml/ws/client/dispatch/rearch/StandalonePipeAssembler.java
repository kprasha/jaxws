/*
 * Copyright (c) 2006 Your Corporation. All Rights Reserved.
 */
package com.sun.xml.ws.client.dispatch.rearch;

import com.sun.xml.ws.api.WSEndpoint;
import com.sun.xml.ws.api.WSService;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.PipelineAssembler;
import com.sun.xml.ws.sandbox.impl.TestDecoderImpl;
import com.sun.xml.ws.sandbox.impl.TestEncoderImpl;
import com.sun.xml.ws.transport.http.client.HttpTransportPipe;
import com.sun.xml.ws.util.pipe.DumpPipe;

public class StandalonePipeAssembler implements PipelineAssembler {
    public Pipe createClient(WSDLPort wsdlModel, WSService service) {
        Pipe p = createTransport(wsdlModel,service);
        return p;
    }

    protected Pipe createTransport(WSDLPort wsdlModel, WSService service) {
        Pipe p = new HttpTransportPipe(TestEncoderImpl.INSTANCE, TestDecoderImpl.INSTANCE11);
        return p;
    }

    public Pipe createServer(WSDLPort wsdlModel, WSEndpoint endpoint, Pipe terminal) {
        return terminal;
    }
}
