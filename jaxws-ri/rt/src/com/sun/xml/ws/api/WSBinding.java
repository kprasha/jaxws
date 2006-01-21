package com.sun.xml.ws.api;

import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.pipe.Pipe;

import javax.xml.ws.Binding;

/**
 * JAX-WS implementation of {@link Binding}.
 *
 * @author Kohsuke Kawaguchi
 */
public interface WSBinding extends Binding {
    /**
     * Gets the SOAP version of this binding.
     *
     * @return
     *      If the binding is using SOAP, this method returns
     *      a {@link SOAPVersion} constant.
     *
     *      If the binding is not based on SOAP, this method
     *      returns null. See {@link Message} for how a non-SOAP
     *      binding shall be handled by {@link Pipe}s.
     */
    SOAPVersion getSOAPVersion();
}
