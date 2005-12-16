/*
 * Copyright (c) 2005 Your Corporation. All Rights Reserved.
 */
package com.sun.xml.ws.client;

import com.sun.xml.ws.sandbox.pipe.Pipe;
import com.sun.xml.ws.sandbox.impl.TestEncoderImpl;
import com.sun.xml.ws.sandbox.impl.TestDecoderImpl;
import com.sun.xml.ws.sandbox.message.Message;
import com.sun.xml.ws.transport.http.client.HttpTransportPipe;

import java.util.List;
import java.util.LinkedList;
import java.util.Map;

public class DispatchPipes {

    LinkedList<Pipe> pipes = new LinkedList();

    public void assemblePipes(Map<String, Object> context) {
        HttpTransportPipe pipe = new HttpTransportPipe(TestEncoderImpl.INSTANCE, TestDecoderImpl.INSTANCE11, context);
        pipes.add(pipe);
    }

    public Message process(Message msg){
        for (Pipe pipe : pipes){
           Message response = pipe.process(msg);
           msg = response;
        }
        return msg;
    }
}
