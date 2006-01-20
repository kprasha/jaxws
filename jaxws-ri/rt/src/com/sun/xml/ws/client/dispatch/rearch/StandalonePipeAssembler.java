/*
 * Copyright (c) 2006 Your Corporation. All Rights Reserved.
 */
package com.sun.xml.ws.client.dispatch.rearch;

import com.sun.xml.ws.api.WSEndpoint;
import com.sun.xml.ws.api.WSService;
import com.sun.xml.ws.api.model.RuntimeModel;
import com.sun.xml.ws.api.model.wsdl.WSDLModel;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.PipelineAssembler;
import com.sun.xml.ws.sandbox.impl.TestDecoderImpl;
import com.sun.xml.ws.sandbox.impl.TestEncoderImpl;
import com.sun.xml.ws.transport.http.client.HttpTransportPipe;

public class StandalonePipeAssembler implements PipelineAssembler {
    public Pipe createClient(RuntimeModel model, WSDLModel wsdlModel, WSService service) {
        return new HttpTransportPipe(TestEncoderImpl.INSTANCE, TestDecoderImpl.INSTANCE11);
    }

    public Pipe createServer(RuntimeModel model, WSDLModel wsdlModel, WSEndpoint endpoint, Pipe terminal) {
        // TODO
        throw new UnsupportedOperationException();
    }
}
