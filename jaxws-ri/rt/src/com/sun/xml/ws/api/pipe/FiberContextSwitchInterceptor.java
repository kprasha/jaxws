package com.sun.xml.ws.api.pipe;

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Interception for {@link Fiber} context switch.
 *
 * TODO: doc improvement!
 *
 * @author Kohsuke Kawaguchi
 */
public interface FiberContextSwitchInterceptor {
    /**
     * Allows the interception of the fiber execution.
     *
     * <p>
     * This method needs to be implemented like this:
     *
     * <pre>
     * &lt;R,P> R execute( Fiber f, P p, Work&lt;R,P> work ) {
     *   // do some preparation work
     *   ...
     *   try {
     *     // invoke
     *     return work.execute(p);
     *   } finally {
     *     // do some clean up work
     *     ...
     *   }
     * }
     * </pre>
     *
     * <p>
     * While somewhat unintuitive,
     * this interception mechanism enables the interceptor to wrap
     * the whole fiber execution into a {@link AccessController#doPrivileged(PrivilegedAction)},
     * for example.
     *
     * @param f
     *      {@link Fiber} to be executed.
     * @param p
     *      The opaque parameter value for {@link Work}. Simply pass this value to
     *      {@link Work#execute(Object)}.
     * @return
     *      The opaque return value from the the {@link Work}. Simply return
     *      the value from {@link Work#execute(Object)}.
     */
    <R,P> R execute( Fiber f, P p, Work<R,P> work );

    /**
     * Abstraction of the execution that happens inside the interceptor.
     */
    interface Work<R,P> {
        /**
         * Have the current thread executes the current fiber,
         * and returns when it stops doing so.
         *
         * <p>
         * The parameter and the return value is controlled by the
         * JAX-WS runtime, and interceptors should simply treat
         * them as opaque values.
         */
        R execute(P param);
    }
}
