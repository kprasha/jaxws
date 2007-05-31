/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License).  You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the license at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * 
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.xml.ws.api.server;

import com.sun.istack.NotNull;
import com.sun.istack.Nullable;

/**
 * Callback interface to signal JAX-WS RI that the processing of an asynchronous request is complete.
 *
 * <p>
 * The application is responsible for invoking one of the two defined methods to
 * indicate the result of the request processing.
 *
 * <p>
 * Both methods will return immediately, and the JAX-WS RI will
 * send out an actual response at some later point.
 *
 * @author Jitendra Kotamraju
 * @author Kohsuke Kawaguchi
 * @since 2.1
 * @see AsyncProvider
 */
public interface AsyncProviderCallback<T> {
    /**
     * Indicates that a request was processed successfully.
     *
     * @param response
     *      Represents an object to be sent back to the client
     *      as a response. To indicate one-way, response needs to be null
     */
    void send(@Nullable T response);

    /**
     * Indicates that an error had occured while processing a request.
     *
     * @param t
     *      The error is propagated to the client. For example, if this is
     *      a SOAP-based web service, the server will send back a SOAP fault.
     */
    void sendError(@NotNull Throwable t);
}
