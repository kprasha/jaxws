/*
 * Copyright (c) 2006 Sun Microsystems Corporation. All Rights Reserved.
 */
package com.sun.xml.ws.handler;

import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.PipeCloner;
import com.sun.xml.ws.api.pipe.helper.AbstractPipeImpl;
import com.sun.xml.ws.api.pipe.helper.AbstractFilterPipeImpl;

/**
 * @author WS Development team
 */

public abstract class HandlerPipe extends AbstractFilterPipeImpl {
    
    //Todo: JavaDocs need updating-
    //Todo: client and server pipes?
    //todo:needs impl
    
    /**
     * handle hold reference to other Pipe for inter-pipe communication
     */
    protected HandlerPipe cousinPipe;
    protected HandlerPipeExchange exchange;
    
    public HandlerPipe(Pipe next) {
        super(next);
    }
    
    public HandlerPipe(Pipe next, HandlerPipe cousinPipe) {
        super(next);
        this.cousinPipe = cousinPipe;
    }
    
    /**
     * Copy constructor for {@link com.sun.xml.ws.api.pipe.Pipe#copy(com.sun.xml.ws.api.pipe.PipeCloner)}.
     */
    protected HandlerPipe(HandlerPipe that, PipeCloner cloner) {
        super(that,cloner);
    }
    
    /**
     * Close SOAPHandlers first and then LogicalHandlers
     */
    public abstract void close();
    
    
    /**
     * This class is used primarily to exchange information or status between 
     * LogicalHandlerPipe and SOAPHandlerPipe
     */
    
    public class HandlerPipeExchange {
     //TODO: get the requirements from different scenarios
     String dummy;    
    }
    
}
