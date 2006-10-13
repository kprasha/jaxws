package com.sun.xml.ws.developer;

import javax.xml.ws.spi.WebServiceFeatureAnnotation;
import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

/**
 * Designates a stateful {@link javax.jws.WebService}.
 * A service class that has this feature on will behave as a stateful web service.
 * See {@link StatefulWebServiceManager} for more details.
 *
 * @since 2.1
 */

@Retention(RUNTIME)
@Target(TYPE)
@WebServiceFeatureAnnotation(id = StatefulFeature.ID, bean = StatefulFeature.class)
public @interface Stateful {
    
}
