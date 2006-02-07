package com.sun.xml.ws.api;

import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.Encoder;
import com.sun.xml.ws.api.pipe.Decoder;

import javax.xml.ws.Binding;

/**
 * JAX-WS implementation of {@link Binding}.
 *
 * <p>
 * Binding conceptually includes the on-the-wire format of the message,
 * this this object owns {@link Encoder} and {@link Decoder}.
 *
 * @author Kohsuke Kawaguchi
 */
public interface WSBinding extends Binding {
    /**
     * Gets the SOAP version of this binding.
     *
     * TODO: clarify what to do with XML/HTTP binding
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

    /**
     * Creates a new {@link Encoder} for this binding.
     *
     * @return
     *      Always non-null.
     */
    Encoder createEncoder();

    /**
     * Creates a new {@link Decoder} for this binding.
     *
     * @return
     *      Always non-null.
     */
    Decoder createDecoder();
}
