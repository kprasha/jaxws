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
import com.sun.xml.ws.transport.http.HttpAdapterList;

import javax.xml.ws.wsaddressing.W3CEndpointReferenceBuilder;
import java.util.List;

/**
 * Represents an object scoped to the current "module" (like a JavaEE web appliation).
 *
 * <p>
 * This object can be obtained from {@link Container#getSPI(Class)}.
 *
 * <p>
 * The scope of the module is driven by {@link W3CEndpointReferenceBuilder#build()}'s
 * requirement that we need to identify a {@link WSEndpoint} that has a specific
 * service/port name.
 *
 * <p>
 * For JavaEE containers this should be scoped to a JavaEE application. For
 * other environment, this could be scoped to any similar notion. If no such
 * notion is available, the implementation of {@link Container} can return
 * a new {@link Module} object each time {@link Container#getSPI(Class)} is invoked.
 *
 * <p>
 * There's a considerable overlap between this and {@link HttpAdapterList}.
 * The SPI really needs to be reconsidered 
 * 
 *
 * @see Container
 * @author Kohsuke Kawaguchi
 * @since 2.1 EA3
 */
public abstract class Module {
    /**
     * Gets the list of {@link BoundEndpoint} deployed in this module.
     *
     * <p>
     * From the point of view of the {@link Module} implementation,
     * it really only needs to provide a {@link List} object as a data store.
     * JAX-WS will update this list accordingly. 
     *
     * @return
     *      always return the same non-null instance.
     */
    public abstract @NotNull List<BoundEndpoint> getBoundEndpoints();
}
