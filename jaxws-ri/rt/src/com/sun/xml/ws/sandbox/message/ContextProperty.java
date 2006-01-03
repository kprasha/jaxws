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
package com.sun.xml.ws.sandbox.message;

import javax.xml.ws.handler.MessageContext;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.util.Map;

/**
 * Marks a field on {@link MessageProperties} as a
 * property of {@link MessageContext}.
 *
 * <p>
 * To make the runtime processing easy, this annotation
 * must be on a public field (since the property value
 * can be set through {@link Map} anyway, you won't be
 * losing abstraction by doing so.)
 *
 * <p>
 * For similar reason, this annotation can be only placed
 * on a reference type, not primitive type.
 *
 * @see MessageProperties
 * @author Kohsuke Kawaguchi
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@interface ContextProperty {
    /**
     * Name of the property.
     */
    String value();
}
