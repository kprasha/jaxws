package com.sun.xml.ws.client.port;

import com.sun.xml.ws.api.model.Mode;
import com.sun.xml.ws.api.model.Parameter;

import javax.xml.ws.Holder;

/**
 * Gets a value from
 *
 * <p>
 * This abstraction hides the handling of {@link Holder}.
 *
 * @author Kohsuke Kawaguchi
 */
enum ValueGetter {
    /**
     * Creates {@link ValueGetter} that works for {@link Mode#IN}  parameter.
     */
    PLAIN() {
        Object get(Object parameter) {
            return parameter;
        }
    },
    /**
     * Creates {@link ValueGetter} that works for {@link Holder},
     * which is  {@link Mode#INOUT} or  {@link Mode#OUT}.
     */
    HOLDER() {
        Object get(Object parameter) {
            // TODO: handler array[idx]==null more gracefully
            return ((Holder)parameter).value;
        }
    };


    abstract Object get(Object parameter);

    static ValueGetter get(Parameter p) {
        if(p.getMode()== Mode.IN)
            return PLAIN;
        else
            return HOLDER;
    }
}
