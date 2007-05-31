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

package com.sun.xml.ws.api.client;

import com.sun.istack.NotNull;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.ClientPipeAssemblerContext;

/**
 * Allow the container (primarily Glassfish) to inject
 * their own pipes into the client pipeline.
 *
 * <p>
 * This interface has a rather ad-hoc set of methods, because
 * we didn't want to define an autonomous pipe-assembly process.
 * (We thought this is a smaller evil compared to that.)
 *
 * <p>
 * JAX-WS obtains this through {@link com.sun.xml.ws.api.server.Container#getSPI(Class)}.
 *
 * @author Jitendra Kotamraju
 */
public abstract class ClientPipelineHook {

    /**
     * Called during the pipeline construction process once to allow a container
     * to register a pipe for security.
     *
     * This pipe will be injected to a point very close to the transport, allowing
     * it to do some security operations.
     *
     * @param ctxt
     *      Represents abstraction of SEI, WSDL abstraction etc. Context can be used
     *      whether add a new pipe to the head or not.
     *
     * @param tail
     *      Head of the partially constructed pipeline. If the implementation
     *      wishes to add new pipes, it should do so by extending
     *      {@link com.sun.xml.ws.api.pipe.helper.AbstractFilterPipeImpl} and making sure that this {@link com.sun.xml.ws.api.pipe.Pipe}
     *      eventually processes messages.
     *
     * @return
     *      The default implementation just returns <tt>tail</tt>, which means
     *      no additional pipe is inserted. If the implementation adds
     *      new pipes, return the new head pipe.
     */
    public @NotNull Pipe createSecurityPipe(ClientPipeAssemblerContext ctxt, @NotNull Pipe tail) {
        return tail;
    }
}
