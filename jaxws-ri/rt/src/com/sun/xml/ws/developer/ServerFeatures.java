package com.sun.xml.ws.developer;

import javax.jws.WebService;
import javax.xml.ws.BindingType;
import javax.xml.ws.Feature;

/**
 * List of server-side features implemented by the JAX-WS RI.
 *
 * <p>
 * These features can be specified to services like the following
 * to active JAX-WS RI specific features:
 *
 * <pre>
 * &#64;{@link BindingType}(features=@{@link Feature}({@link ServerFeatures}.XYZ))
 * </pre>
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class ServerFeatures {
    private ServerFeatures() {} // no instanciation please

    /**
     * Designates a stateful {@link WebService}.
     * A service class that has this feature on will behave as a stateful web service.
     * See {@link StatefulWebServiceManager} for more details.
     * @since 2.1
     */
    public static final String STATEFUL = "http://jax-ws.dev.java.net/features/stateful";
}
