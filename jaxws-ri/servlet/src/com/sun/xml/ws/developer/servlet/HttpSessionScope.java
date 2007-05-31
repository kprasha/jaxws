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

package com.sun.xml.ws.developer.servlet;

import com.sun.xml.ws.api.server.InstanceResolverAnnotation;
import com.sun.xml.ws.server.servlet.HttpSessionInstanceResolver;

import javax.jws.WebService;
import javax.servlet.http.HttpSession;
import javax.xml.ws.spi.WebServiceFeatureAnnotation;
import java.lang.annotation.Documented;
import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

/**
 * Designates a service class that should be tied to {@link HttpSession} scope.
 *
 * <p>
 * When a service class is annotated with this annotation like the following,
 * the JAX-WS RI runtime will instanciate a new instance of the service class for
 * each {@link HttpSession}.
 *
 * <pre>
 * &#64;{@link WebService}
 * &#64;{@link HttpSessionScope}
 * class CounterService {
 *     protected int count = 0;
 *
 *     public CounterService() {}
 *
 *     public int inc() {
 *         return count++;
 *     }
 * }
 * </pre>
 *
 * <p>
 * This allows you to use instance fields for storing per-session state
 * (in the above example, it will create a separate counter for each client.)
 *
 * <p>
 * The service instance will be GCed when the corresponding {@link HttpSession}
 * is GCed. Refer to servlet documentation for how to configure the timeout behavior.
 *
 * @author Kohsuke Kawaguchi
 * @since JAX-WS 2.1
 */
@Retention(RUNTIME)
@Target(TYPE)
@Documented
@WebServiceFeatureAnnotation(id=HttpSessionScopeFeature.ID, bean=HttpSessionScopeFeature.class)
@InstanceResolverAnnotation(HttpSessionInstanceResolver.class)
public @interface HttpSessionScope {
}
