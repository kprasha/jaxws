/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 *
 * You can obtain a copy of the license at
 * https://jwsdp.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * HEADER in each file and include the License file at
 * https://jwsdp.dev.java.net/CDDLv1.0.html  If applicable,
 * add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your
 * own identifying information: Portions Copyright [yyyy]
 * [name of copyright owner]
 */
package com.sun.xml.ws.sandbox;

import com.sun.xml.ws.sandbox.message.Message;
import com.sun.xml.ws.api.pipe.Pipe;

/**
 * Represents a component that listens to the transport (such as
 * HTTP-via-servlet, HTTP-via-LWhttpserver, TCP, and SMTP) and
 * accepts a {@link Message}.
 *
 * <p>
 * This object is responsible for the following work:
 * <ol>
 *  <li>Listens to the transport and creates a {@link Message}.
 *  <li>Sends it to {@link Pipe} for processing, and receives a reply {@link Message}.
 *  <li>Sends a reply {@link Message} to the transport.
 * </ol>
 *
 * <p>
 * An {@link Acceptor} receives a {@link Pipe} object when it's constructed,
 * so there is no setter method for it. The {@link Pipe} object that the {@link Acceptor}
 * owns do not need to be exposed to outside, so there's no setter method.
 *
 * <p>
 * (In JAX-RPC, this component was called 'Tie'.)
 *
 * TODO: how an acceptor is created?
 */
public interface Acceptor {
    /**
     * Stops accepting new messages.
     * This method is used to "unpublish" an endpoint.
     *
     * <p>
     * Since this method is used as a part of clean-up, any error should be
     * just logged and be recovered.
     */
    void close();
}
