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
import com.sun.xml.ws.server.DefaultResourceInjector;

import javax.annotation.PostConstruct;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.WebServiceException;

/**
 * Represents a functionality of the container to inject resources
 * to application service endpoint object (usually but not necessarily as per JavaEE spec.)
 *
 * <p>
 * If {@link Container#getSPI(Class)} returns a valid instance of {@link ResourceInjector},
 * The JAX-WS RI will call the {@link #inject} method for each service endpoint
 * instance that it manages.
 *
 * <p>
 * The JAX-WS RI will be responsible for calling {@link PostConstruct} callback,
 * so implementations of this class need not do so.
 *
 * @author Kohsuke Kawaguchi
 * @see Container
 */
public abstract class ResourceInjector {
    /**
     * Performs resource injection.
     *
     * @param context
     *      {@link WebServiceContext} implementation to be injected into the instance.
     * @param instance
     *      Instance of the service endpoint class to which resources will be injected.
     *
     * @throws WebServiceException
     *      If the resource injection fails.
     */
    public abstract void inject(@NotNull WSWebServiceContext context, @NotNull Object instance);

    /**
     * Fallback {@link ResourceInjector} implementation used when the {@link Container}
     * doesn't provide one.
     *
     * <p>
     * Just inject {@link WSWebServiceContext} and done.
     */
    public static final ResourceInjector STANDALONE = new DefaultResourceInjector();
}
