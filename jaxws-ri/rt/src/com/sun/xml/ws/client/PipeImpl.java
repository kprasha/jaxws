/*
 * Copyright (c) 2006 Your Corporation. All Rights Reserved.
 */
package com.sun.xml.ws.client;

import com.sun.xml.ws.sandbox.message.Message;
import com.sun.xml.ws.sandbox.pipe.Pipe;
import com.sun.xml.ws.sandbox.pipe.PipeCloner;

//Todo: does this need to be abstract? --here simple to get Dispatch going`

public abstract class PipeImpl implements Pipe {
    /**
     * Sends a {@link com.sun.xml.ws.sandbox.message.Message} and returns a response {@link com.sun.xml.ws.sandbox.message.Message} to it.
     *
     * @param msg always a non-null valid unconsumed {@link com.sun.xml.ws.sandbox.message.Message} that
     *            represents a request.
     *            The callee may consume a {@link com.sun.xml.ws.sandbox.message.Message} (and in fact
     *            most of the time it will), and therefore once a {@link com.sun.xml.ws.sandbox.message.Message}
     *            is given to a {@link com.sun.xml.ws.sandbox.pipe.Pipe}.
     * @return If this method returns a non-null value, it must be
     *         a valid unconsumed {@link com.sun.xml.ws.sandbox.message.Message}. This message represents
     *         a response to the request message passed as a parameter.
     *         <p/>
     *         This method is also allowed to return null, which indicates
     *         that there's no response. This is used for things like
     *         one-way message and/or one-way transports.
     * @throws javax.xml.ws.WebServiceException
     *                          On the server side, this signals an error condition where
     *                          a fault reply is in order (or the exception gets eaten by
     *                          the top-most transport {@link com.sun.xml.ws.sandbox.pipe.Pipe} if it's one-way.)
     *                          This frees each {@link com.sun.xml.ws.sandbox.pipe.Pipe} from try/catching a
     *                          {@link javax.xml.ws.WebServiceException} in every layer.
     *                          <p/>
     *                          Note that this method is also allowed to return a {@link com.sun.xml.ws.sandbox.message.Message}
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
    public Message process(Message msg) {
        return null;
    }

    /**
     * Invoked before the last copy of the pipeline is about to be discarded,
     * to give {@link com.sun.xml.ws.sandbox.pipe.Pipe}s a chance to clean up any resources.
     * <p/>
     * This can be used to invoke {@link javax.annotation.PreDestroy} lifecycle methods
     * on user handler. The invocation of it is optional on the client side.
     */
    public void preDestroy() {

    }

    /**
     * Creates an identical clone of this {@link com.sun.xml.ws.sandbox.pipe.Pipe}.
     * <p/>
     * <p/>
     * This method creates an identical pipeline that can be used
     * concurrently with this pipeline. When the caller of a pipeline
     * is multi-threaded and need concurrent use of the same pipeline,
     * it can do so by creating copies through this method.
     * <p/>
     * <h3>Implementation Note</h3>
     * <p/>
     * For most {@link com.sun.xml.ws.sandbox.pipe.Pipe} implementations that delegate to another
     * {@link com.sun.xml.ws.sandbox.pipe.Pipe}, this method requires that you also copy the {@link com.sun.xml.ws.sandbox.pipe.Pipe}
     * that you delegate to.
     * <p/>
     * For limited number of {@link com.sun.xml.ws.sandbox.pipe.Pipe}s that do not maintain any
     * thread unsafe resource, it is allowed to simply return <tt>this</tt>
     * from this method (notice that even if you are stateless, if you
     * got a delegating {@link com.sun.xml.ws.sandbox.pipe.Pipe} and that one isn't stateless, you
     * still have to copy yourself.)
     * <p/>
     * <p/>
     * Note that this method might be invoked by one thread while another
     * thread is executing the {@link #process(com.sun.xml.ws.sandbox.message.Message)} method. See
     * the {@link com.sun.xml.ws.sandbox.Encoder#copy()} for more discussion about this.
     *
     * @param cloner Use this object (in particular its {@link com.sun.xml.ws.sandbox.pipe.PipeCloner#copy(com.sun.xml.ws.sandbox.pipe.Pipe)} method
     *               to clone other pipe references you have
     *               in your pipe. See {@link com.sun.xml.ws.sandbox.pipe.PipeCloner} for more discussion
     *               about why.
     * @param cloner
     * @return always non-null {@link com.sun.xml.ws.sandbox.pipe.Pipe}.
     */
    public Pipe copy(PipeCloner cloner) {
        return null;
    }
}
