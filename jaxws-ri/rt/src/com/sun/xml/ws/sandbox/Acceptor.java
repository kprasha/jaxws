package com.sun.xml.ws.sandbox;

import com.sun.xml.ws.sandbox.message.Message;
import com.sun.xml.ws.sandbox.chain.Chain;

/**
 * Represents a component that listens to the transport (such as
 * HTTP-via-servlet, HTTP-via-LWhttpserver, TCP, and SMTP) and
 * accepts a {@link Message}.
 *
 * <p>
 * This object is responsible for the following work:
 * <ol>
 *  <li>Listens to the transport and creates a {@link Message}.
 *  <li>Sends it to {@link Chain} for processing, and receives a reply {@link Message}.
 *  <li>Sends a reply {@link Message} to the transport.
 * </ol>
 *
 * <p>
 * An {@link Acceptor} receives a {@link Chain} object when it's constructed,
 * so there is no setter method for it. The {@link Chain} object that the {@link Acceptor}
 * owns do not need to be exposed to outside, so there's no setter method.
 *
 * <p>
 * (In JAX-RPC, this component was called 'Tie'.)
 *
 * TODO: how an acceptor is created?
 */
public interface Acceptor {
}
