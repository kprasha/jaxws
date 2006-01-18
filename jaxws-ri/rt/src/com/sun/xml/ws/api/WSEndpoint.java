package com.sun.xml.ws.api;

import com.sun.xml.ws.server.RuntimeEndpointInfo;

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
}
