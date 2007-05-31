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

package com.sun.xml.ws.api;

import com.sun.xml.ws.developer.MemberSubmissionAddressing;
import com.sun.xml.ws.developer.MemberSubmissionAddressingFeature;
import com.sun.xml.ws.developer.Stateful;
import com.sun.xml.ws.developer.StatefulFeature;

import javax.xml.ws.WebServiceFeature;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

/**
 * <p>
 * This annotation should be used on a constructor of classes extending {@link WebServiceFeature} other than
 * Spec defined features, to help JAX-WS runtime recognize feature extensions.
 * </p>
 * <p>
 * For WebServiceFeature annotations to be recognizable by JAX-WS runtime, the feature annotation should point
 * to a corresponding bean (class extending WebServiceFeature). Only one of the constructors in the bean MUST be marked
 * with @FeatureConstructor whose value captures the annotaion attribute names for the corresponding parameters.
 * </p>
 * For example,
 * @see MemberSubmissionAddressingFeature
 * @see MemberSubmissionAddressing
 *
 * @see Stateful
 * @see StatefulFeature
 *
 * @author Rama Pulavarthi
 */
@Retention(RUNTIME)
@Target(ElementType.CONSTRUCTOR)

public @interface FeatureConstructor {
    /**
     * The name of the parameter.
     */
    String[] value() default {};
}
