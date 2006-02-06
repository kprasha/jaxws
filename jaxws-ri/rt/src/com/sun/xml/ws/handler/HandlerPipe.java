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
// TODO: shouldn't this class be abstract? - KK
public class HandlerPipe extends AbstractFilterPipeImpl {
    //Todo: JavaDocs need updating-
    //Todo: client and server pipes?
    //todo:needs impl

    public HandlerPipe(Pipe next) {
        super(next);
    }

    /**
     * Copy constructor for {@link com.sun.xml.ws.api.pipe.Pipe#copy(com.sun.xml.ws.api.pipe.PipeCloner)}.
     */
    protected HandlerPipe(HandlerPipe that, PipeCloner cloner) {
        super(that,cloner);
    }

    /**
     * Sends a {@link com.sun.xml.ws.api.message.Message} and returns a response {@link com.sun.xml.ws.api.message.Message} to it.
     *
     * @param packet
     * @return If this method returns a non-null value, it must be
     *         a valid unconsumed {@link com.sun.xml.ws.api.message.Message}. This message represents
     *         a response to the request message passed as a parameter.
     *         <p/>
     *         This method is also allowed to return null, which indicates
     *         that there's no response. This is used for things like
     *         one-way message and/or one-way transports.
     * @throws javax.xml.ws.WebServiceException
     *                          On the server side, this signals an error condition where
     *                          a fault reply is in order (or the exception gets eaten by
     *                          the top-most transport {@link com.sun.xml.ws.api.pipe.Pipe} if it's one-way.)
     *                          This frees each {@link com.sun.xml.ws.api.pipe.Pipe} from try/catching a
     *                          {@link javax.xml.ws.WebServiceException} in every layer.
     *                          <p/>
     *                          Note that this method is also allowed to return a {@link com.sun.xml.ws.api.message.Message}
     *                          that has a fault as the payload.
     *                          <p/>
     *                          On the client side, the {@link javax.xml.ws.WebServiceException} thrown
     *                          will be propagated all the way back to the calling client
     *                          applications.
     * @throws RuntimeException Other runtime exception thrown by this method must
     *                          be treated as a bug in the pipe implementation,
     *                          and therefore should not be converted into a fault.
     *                          (Otherwise it becomes very difficult to debug implementation
     *                          problems.)
     *                          <p/>
     *                          <p/>
     *                          On the server side, this exception should be most likely
     *                          just logged. On the client-side it gets propagated to the
     *                          client application.
     *                          <p/>
     *                          <p/>
     *                          The consequence of this is that if a pipe calls
     *                          into an user application (such as {@link javax.xml.ws.handler.soap.SOAPHandler}
     *                          or {@link javax.xml.ws.handler.LogicalHandler}), where a {@link RuntimeException}
     *                          is *not* a bug in the JAX-WS implementation, it must be catched
     *                          and wrapped into a {@link javax.xml.ws.WebServiceException}.
     */
    public Packet process(Packet packet) {
        return null;
    }

    /**
     * @param cloner
     * @return
     */
    public Pipe copy(PipeCloner cloner) {
        return new HandlerPipe(this,cloner);
    }
}
