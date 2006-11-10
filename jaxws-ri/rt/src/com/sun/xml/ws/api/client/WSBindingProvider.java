package com.sun.xml.ws.api.client;

import javax.xml.ws.BindingProvider;

/**
 * Extends {@link BindingProvider} with RI specific methods. This object is
 * accessed after creating a proxy or dispatch instance in
 * {@link PortCreationCallback}, {@link ServiceInterceptor} methods. The callback implementor(typically
 * a container) could use this object to add some functionality under the
 * covers.
 *
 * <p>
 * By passing WSBindingProvider instead of BindingProvider to the callback methods,
 * we could add additional functionality in this class.
 *
 * <p>
 * Since dynamic proxy is created using this, this needs be an interface
 *
 * @see PortCreationCallback
 * @see ServiceInterceptor
 *
 * @author Jitendra Kotamraju
 * @deprecated
 *      Use {@link com.sun.xml.ws.developer.WSBindingProvider}
 */
public interface WSBindingProvider extends com.sun.xml.ws.developer.WSBindingProvider {
    // We will add RI-specific methods when there is a need
}
