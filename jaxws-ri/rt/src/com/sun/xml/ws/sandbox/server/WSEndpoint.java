package com.sun.xml.ws.sandbox.server;

import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.spi.runtime.Container;

import javax.xml.ws.WebServiceException;

/**
 * Root object of the composed whole {@link Packet} processing code
 * at the server.
 *
 * <p>
 * One instance of this object is created for each deployed service
 * endpoint.
 *
 * <pre>
 * TODO:
 *      how do we correctly produce WSDL that contains endpoint specific information?
 *      for example, binding needs to be reflected in WSDL, so does the endpoint address.
 * </pre>
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class WSEndpoint<T> {

    /**
     * Represents the binding for which this {@link WSEndpoint}
     * is created for.
     *
     * @return
     *      always non-null, same object.
     */
    public abstract WSBinding getBinding();

    /**
     * Gets the {@link Container} object.
     *
     * @return
     *      always non-null, same object. If there's no valid {@link Container},
     *      {@link Container#NONE} will be returned.
     */
    public abstract Container getContainer();

    /**
     * Gets the port that this endpoint is serving.
     *
     * @return
     *      always non-null. TODO: is it?
     */
    public abstract WSDLPort getPort();

    /**
     * Processes a request and produces a reply.
     *
     * <p>
     * This method takes a {@link Packet} that represents
     * a request, run it through a {@link Pipe}line, eventually
     * pass it to the user implementation code, which produces
     * a reply, then run that through the pipeline again,
     * and eventually return it as a return value.
     *
     * @param request
     *      Must be non-null unconsumed {@link Packet} that represents
     *      a request.
     * @return
     *      always a non-null unconsumed {@link Packet} that represents
     *      a reply to the request.
     *
     * @throws WebServiceException
     *      This method <b>does not</b> throw a {@link WebServiceException}.
     *      The {@link WSEndpoint} must always produce a fault {@link Message}
     *      for it.
     *
     * @throws RuntimeException
     *      A {@link RuntimeException} thrown from this method, including
     *      {@link WebServiceException}, must be treated as a bug in the
     *      code (including JAX-WS and all the pipe implementations), not
     *      an operator error by the user.
     *
     *      <p>
     *      Therefore, it should be recorded by the caller in a way that
     *      allows developers to fix a bug.
     *
     *TODO: define a mechanism to early-close a response channel
     */
    public abstract Packet process(Packet request, WebServiceContextDelegate wscd);

    /**
     * Sets a new {@link InstanceResolver}.
     *
     * @param resolver
     *      Must be non-null.
     */
    public abstract void setInstanceResolver( InstanceResolver<T> resolver );

    public abstract InstanceResolver<T> getInstanceResolver();

    /**
     * Indicates that the {@link WSEndpoint} is about to be turned off,
     * and will no longer serve any packet anymore.
     *
     * <p>
     * This method needs to be invoked for the JAX-WS RI to correctly
     * implement some of the spec semantics (TODO: pointer.)
     * It's the responsibility of the code that "owns" a {@link WSEndpoint}
     * to invoke this method.
     *
     * <p>
     * Once this method is called, the behavior is undefed for
     * all in-progress {@link #process(Packet)} methods (by other threads)
     * and future {@link #process(Packet)} method invocations.
     */
    public abstract void dispose();

    public abstract ServiceDefinition getServiceDefinition();
}
