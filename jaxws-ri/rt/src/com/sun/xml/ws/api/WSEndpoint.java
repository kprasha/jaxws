package com.sun.xml.ws.api;

import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.model.SEIModel;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.server.InstanceResolver;
import com.sun.xml.ws.api.server.ServiceDefinition;
import com.sun.xml.ws.api.server.TransportBackChannel;
import com.sun.xml.ws.api.server.WebServiceContextDelegate;
import com.sun.xml.ws.spi.runtime.Container;

import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.Provider;

/**
 * Root object of the composed whole {@link Packet} processing code
 * at the server.
 *
 * <p>
 * One instance of this object is created for each deployed service
 * endpoint.
 *
 * <p>
 * {@link WSEndpoint} owns a {@link WebServiceContext} implementation
 * (but a bulk of the work is delegated to {@link WebServiceContextDelegate}.)
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
     * <p>
     * A service is not required to have a WSDL, and when it doesn't,
     * this method returns null.
     *
     * @return
     *      Possibly null, but always the same value.
     */
    public abstract WSDLPort getPort();

    /**
     * Gets the {@link SEIModel} that represents the relationship
     * between WSDL and Java SEI.
     *
     * <p>
     * This method returns a non-null value if and only if this
     * endpoint is ultimately serving an application through an SEI.
     * (That is 'T' is the SEI, not {@link Provider}.
     *
     * @return
     *      maybe null. See above for more discussion.
     *      Always the same value.
     */
    public abstract SEIModel getSEIModel();


    public abstract PipeHead createPipeHead();

    public interface PipeHead {
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
         * @param wscd
         *      {@link WebServiceContextDelegate} to be set to {@link Packet}.
         *      (we didn't have to take this and instead just ask the caller to
         *      set to {@link Packet#webServiceContextDelegate}, but that felt
         *      too error prone.)
         * @param tbc
         *      {@link TransportBackChannel} to be set to {@link Packet}.
         *      See the {@code wscd} parameter javadoc for why this is a parameter.
         *      Can be null.
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
         */
        Packet process(
            Packet request, WebServiceContextDelegate wscd, TransportBackChannel tbc);
    }

    /**
     * Gets the instance resolver that determines which object ultimately
     * consumes the message.
     *
     * @return
     *      always non-null same object.
     */
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
     * all in-progress {@link PipeHead#process} methods (by other threads)
     * and future {@link PipeHead#process} method invocations.
     */
    public abstract void dispose();

    /**
     * Gets the description of the service.
     *
     * <p>
     * A service is not required to have a description, and when it doesn't,
     * this method returns null.
     *
     * @return
     *      Possible null, but always the same value.
     */
    public abstract ServiceDefinition getServiceDefinition();
}
