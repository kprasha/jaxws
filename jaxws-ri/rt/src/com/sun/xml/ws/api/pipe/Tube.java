package com.sun.xml.ws.api.pipe;

import com.sun.xml.ws.api.message.Packet;

import javax.annotation.PreDestroy;
import javax.xml.ws.WebServiceException;

/**
 * @author Kohsuke Kawaguchi
 * @author Jitendra Kotamraju
 */
public interface Tube {
    NextAction processRequest(Packet p);
    NextAction processResponse(Packet p);

    /**
     * Invoked before the last copy of the pipeline is about to be discarded,
     * to give {@link Tube}s a chance to clean up any resources.
     *
     * <p>
     * This can be used to invoke {@link PreDestroy} lifecycle methods
     * on user handler. The invocation of it is optional on the client side,
     * but mandatory on the server side.
     *
     * <p>
     * When multiple copies of pipelines are created, this method is called
     * only on one of them.
     *
     * @throws WebServiceException
     *      If the clean up fails, {@link WebServiceException} can be thrown.
     *      This exception will be propagated to users (if this is client),
     *      or recorded (if this is server.)
     */
    void preDestroy();

    /**
     * Creates an identical clone of this {@link Tube}.
     *
     * <p>
     * This method creates an identical pipeline that can be used
     * concurrently with this pipeline. When the caller of a pipeline
     * is multi-threaded and need concurrent use of the same pipeline,
     * it can do so by creating copies through this method.
     *
     * <h3>Implementation Note</h3>
     * <p>
     * It is the implementation's responsibility to call
     * {@link PipeCloner#add(Pipe,Pipe)} to register the copied pipe
     * with the original. This is required before you start copying
     * the other {@link Pipe} references you have, or else there's a
     * risk of infinite recursion.
     * <p>
     * For most {@link Pipe} implementations that delegate to another
     * {@link Pipe}, this method requires that you also copy the {@link Pipe}
     * that you delegate to.
     * <p>
     * For limited number of {@link Pipe}s that do not maintain any
     * thread unsafe resource, it is allowed to simply return <tt>this</tt>
     * from this method (notice that even if you are stateless, if you
     * got a delegating {@link Pipe} and that one isn't stateless, you
     * still have to copy yourself.)
     *
     * <p>
     * Note that this method might be invoked by one thread while another
     * thread is executing the {@link #process(Packet)} method. See
     * the {@link Codec#copy()} for more discussion about this.
     *
     * @param cloner
     *      Use this object (in particular its {@link PipeCloner#copy(Pipe)} method
     *      to clone other pipe references you have
     *      in your pipe. See {@link PipeCloner} for more discussion
     *      about why.
     *
     * @return
     *      always non-null {@link Pipe}.
     * @param cloner
     */
    Tube copy(PipeCloner cloner);
}
