package com.sun.xml.ws.sandbox.server;

import com.sun.xml.ws.spi.runtime.Container;
import com.sun.xml.ws.api.WSBinding;
import java.net.URL;
import javax.xml.namespace.QName;

import javax.xml.ws.Provider;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;
import org.xml.sax.EntityResolver;

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
     * @param url
     *      Optional WSDL URL. If none exists, this parameter can be null.
     * @param resolver
     *      Optional resolver used to de-reference resources referenced from
     *      WSDL. Must be null if the {@code url} is null.
     * @param serviceName
     *      Optional service name to override the one given by the implementation clas.
     * @param portName
     *      Optional port name to override the one given by the implementation clas.
     *      
     * TODO: DD has a configuration for MTOM threshold.
     * Maybe we need something more generic so that other technologies
     * like Tango can get information from DD.
     *
     * @return newly constructed {@link WSEndpoint}.
     * @throws WebServiceException
     *      if the endpoint set up fails.
     */
    public <T> WSEndpoint<T> createSEIEndpoint(Class<T> seiType, T implementationObject, QName serviceName, QName portName, Container container, WSBinding binding, URL wsdl, EntityResolver resolver) {
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
