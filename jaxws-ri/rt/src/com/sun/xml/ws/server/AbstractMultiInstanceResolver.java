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

package com.sun.xml.ws.server;

import com.sun.xml.ws.api.server.InstanceResolver;
import com.sun.xml.ws.api.server.ResourceInjector;
import com.sun.xml.ws.api.server.WSEndpoint;
import com.sun.xml.ws.api.server.WSWebServiceContext;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.reflect.Method;

/**
 * Partial implementation of {@link InstanceResolver} with code
 * to handle multiple instances per server.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class AbstractMultiInstanceResolver<T> extends AbstractInstanceResolver<T> {
    protected final Class<T> clazz;

    // fields for resource injection.
    private /*almost final*/ WSWebServiceContext webServiceContext;
    protected /*almost final*/ WSEndpoint owner;
    private final Method postConstructMethod;
    private final Method preDestroyMethod;
    private /*almost final*/ ResourceInjector resourceInjector;

    public AbstractMultiInstanceResolver(Class<T> clazz) {
        this.clazz = clazz;

        postConstructMethod = findAnnotatedMethod(clazz, PostConstruct.class);
        preDestroyMethod = findAnnotatedMethod(clazz, PreDestroy.class);
    }

    /**
     * Perform resource injection on the given instance.
     */
    protected final void prepare(T t) {
        // we can only start creating new instances after the start method is invoked.
        assert webServiceContext!=null;

        resourceInjector.inject(webServiceContext,t);
        invokeMethod(postConstructMethod,t);
    }

    /**
     * Creates a new instance via the default constructor.
     */
    protected final T create() {
        T t = createNewInstance(clazz);
        prepare(t);
        return t;
    }

    @Override
    public void start(WSWebServiceContext wsc, WSEndpoint endpoint) {
        resourceInjector = getResourceInjector(endpoint);
        this.webServiceContext = wsc;
        this.owner = endpoint;
    }

    protected final void dispose(T instance) {
        invokeMethod(preDestroyMethod,instance);
    }
}
