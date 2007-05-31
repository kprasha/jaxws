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

package com.sun.xml.ws.developer;

import com.sun.xml.ws.api.server.InstanceResolverAnnotation;
import com.sun.xml.ws.server.StatefulInstanceResolver;

import javax.jws.WebService;
import javax.xml.ws.spi.WebServiceFeatureAnnotation;
import java.lang.annotation.Documented;
import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

/**
 * Designates a stateful {@link WebService}.
 *
 * <p>
 * A service class that has this feature on will behave as a stateful web service.
 * See {@link StatefulWebServiceManager} for more about stateful web service.
 *
 * @since 2.1
 * @ses StatefulWebServiceManager
 */
@Retention(RUNTIME)
@Target(TYPE)
@Documented
@WebServiceFeatureAnnotation(id = StatefulFeature.ID, bean = StatefulFeature.class)
@InstanceResolverAnnotation(StatefulInstanceResolver.class)
public @interface Stateful {    
}
