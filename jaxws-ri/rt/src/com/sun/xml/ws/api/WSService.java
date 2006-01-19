package com.sun.xml.ws.api;

import com.sun.xml.ws.client.WSServiceDelegate;
import com.sun.xml.ws.spi.runtime.Container;

import javax.xml.ws.spi.ServiceDelegate;
import javax.xml.ws.Service;

/**
 * JAX-WS implementation of {@link ServiceDelegate}.
 *
 * <p>
 * This abstract class is used only to improve the static type safety
 * of the JAX-WS internal API.
 *
 * <p>
 * The class name intentionally doesn't include "Delegate",
 * because the fact that it's a delegate is a detail of
 * the JSR-224 API, and for the layers above us this object
 * nevertheless represents {@link Service}. We want them
 * to think of this as an internal representation of a service.
 *
 * <p>
 * Only JAX-WS internal code may downcast this to {@link WSServiceDelegate}.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class WSService extends ServiceDelegate {
    protected WSService() {
    }
}
