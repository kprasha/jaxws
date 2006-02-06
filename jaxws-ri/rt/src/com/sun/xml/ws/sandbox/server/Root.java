package com.sun.xml.ws.sandbox.server;

import com.sun.xml.ws.spi.runtime.Container;
import com.sun.xml.ws.api.WSBinding;

import javax.xml.ws.Provider;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;

/**
 * Entry point to the JAX-WS RI server-side runtime.
 * TODO: rename.
 *
 * TODO: more create method.
 *
 * @author Kohsuke Kawaguchi
 */
public class Root {
    /*
    no need to take WebServiceContext implementation. That's hidden inside our system.
    We shall only take delegate to getUserPrincipal and isUserInRole from adapter. 
    */

    /**
     * Used for "from-Java" deployment.
     *
     * <p>
     * This method works like the following:
     * <ol>
     * <li>{@link ServiceDefinition} is modeleed from the given SEI type.
     * <li>{@link InstanceResolver} that always serves <tt>implementationObject</tt> will be used.
     * <li>TODO: where does the binding come from?
     * </ol>
     *
     * @return newly constructed {@link WSEndpoint}.
     * @throws WebServiceException
     *      if the endpoint set up fails.
     */
    public <T> WSEndpoint<T> createSEIEndpoint(Class<T> seiType, T implementationObject, Container container, WSBinding binding) {
        // TODO: implement this method later
        throw new UnsupportedOperationException();
    }

    /**
     * Creates a {@link Provider}-backed {@link WSEndpoint}.
     *
     * <p>
     * This method works like the following:
     * <ol>
     * <li>TODO: where does {@link ServiceDefinition} come from?
     * <li>TODO: where does the binding come from?
     * </ol>
     *
     * @return newly constructed {@link WSEndpoint}.
     * @throws WebServiceException
     *      if the endpoint set up fails.
     */
    public <T> WSEndpoint<Provider<T>> createProviderEndpoint(
        Class<T> clazz, Service.Mode mode, InstanceResolver<Provider<T>> resolver,
        Container container, WSBinding binding) {

        // TODO: implement this method later
        throw new UnsupportedOperationException();
    }
}
