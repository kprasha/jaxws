package com.sun.xml.ws.developer;

import javax.jws.WebService;
import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

/**
 * Designates a stateful {@link WebService}.
 *
 * @author Kohsuke Kawaguchi
 * @see StatefulWebServiceManager
 * @since 2.1
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface Stateful {
}
