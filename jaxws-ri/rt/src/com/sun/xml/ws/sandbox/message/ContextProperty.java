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
