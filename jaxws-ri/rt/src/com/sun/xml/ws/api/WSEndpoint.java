package com.sun.xml.ws.api;

import com.sun.xml.ws.server.RuntimeEndpointInfo;
import com.sun.xml.ws.spi.runtime.Container;

import javax.xml.ws.Endpoint;

/**
 * JAX-WS implementation of {@link Endpoint}.
 *
 * <p>
 * This object is an entry point to various environment information
 * for components running on the server side.
 *
 * <p>
 * Only JAX-WS internal code may downcast this to {@link RuntimeEndpointInfo}.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class WSEndpoint extends Endpoint {

    public abstract WSBinding getBinding();

    /**
     * Gets the {@link Container} object.
     *
     * @return
     *      non-null if this {@link WSService} is running inside a container.
     *      This is typically when a service is deployed by the container.
     *      <p>
     *      This method always returns the same value for multiple invocations.
     */
    public abstract Container getContainer();
}
