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
import com.sun.istack.Nullable;
import com.sun.xml.ws.api.WSService;
import com.sun.xml.ws.util.ServiceFinder;

import javax.xml.ws.Service;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Creates {@link ServiceInterceptor}.
 *
 * <p>
 * Code that wishes to inject {@link ServiceInterceptor} into {@link WSService}
 * must implement this class. There are two ways to have the JAX-WS RI
 * recognize your {@link ServiceInterceptor}s.
 *
 * <h3>Use {@link ServiceFinder}</h3>
 * <p>
 * {@link ServiceInterceptorFactory}s discovered via {@link ServiceFinder}
 * will be incorporated to all {@link WSService} instances.
 *
 * <h3>Register per-thread</h3>
 *
 *
 * @author Kohsuke Kawaguchi
 * @see ServiceInterceptor
 * @see 2.1 EA3
 */
public abstract class ServiceInterceptorFactory {
    public abstract ServiceInterceptor create(@NotNull WSService service);

    /**
     * Loads all {@link ServiceInterceptor}s and return aggregated one.
     */
    public static @NotNull ServiceInterceptor load(@NotNull WSService service, @Nullable ClassLoader cl) {
        List<ServiceInterceptor> l = new ArrayList<ServiceInterceptor>();

        // first service look-up
        for( ServiceInterceptorFactory f : ServiceFinder.find(ServiceInterceptorFactory.class))
            l.add(f.create(service));

        // then thread-local
        for( ServiceInterceptorFactory f : threadLocalFactories.get())
            l.add(f.create(service));

        return ServiceInterceptor.aggregate(l.toArray(new ServiceInterceptor[l.size()]));
    }

    private static ThreadLocal<Set<ServiceInterceptorFactory>> threadLocalFactories = new ThreadLocal<Set<ServiceInterceptorFactory>>() {
        protected Set<ServiceInterceptorFactory> initialValue() {
            return new HashSet<ServiceInterceptorFactory>();
        }
    };

    /**
     * Registers {@link ServiceInterceptorFactory} for this thread.
     *
     * <p>
     * Once registered, {@link ServiceInterceptorFactory}s are consulted for every
     * {@link Service} created in this thread, until it gets unregistered.
     */
    public static boolean registerForThread(ServiceInterceptorFactory factory) {
        return threadLocalFactories.get().add(factory);
    }

    /**
     * Removes previously registered {@link ServiceInterceptorFactory} for this thread.
     */
    public static boolean unregisterForThread(ServiceInterceptorFactory factory) {
        return threadLocalFactories.get().remove(factory);
    }
}
