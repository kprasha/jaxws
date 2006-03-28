package com.sun.xml.ws.api;

import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.Encoder;
import com.sun.xml.ws.api.pipe.Decoder;
import com.sun.istack.NotNull;

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
     * <p>
     * This is just a shor-cut for  {@code getBindingID().getSOAPVersion()}
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
     * <p>
     * This is just a short-cut for {@code getBindingID().createEncoder()}
     */
    @NotNull Encoder createEncoder();

    /**
     * Creates a new {@link Decoder} for this binding.
     *
     * <p>
     * This is just a short-cut for {@code getBindingID().createDecoder()}
     */
    @NotNull Decoder createDecoder();

    /**
     * Gets the binding ID, which uniquely identifies the binding.
     *
     * <p>
     * The relevant specs define the binding IDs and what they mean.
     * The ID is used in many places to identify the kind of binding
     * (such as SOAP1.1, SOAP1.2, REST, ...)
     *
     * @return
     *      Always non-null same value.
     */
    @NotNull BindingID getBindingId();
}
