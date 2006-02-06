package com.sun.xml.ws.sandbox.server;

import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.message.Message;

/**
 * Determines the instance that serves
 * the given request packet.
 *
 * <p>
 * The JAX-WS spec always use a singleton instance
 * to serve all the requests, but this hook provides
 * a convenient way to route messages to a proper receiver.
 *
 * <p>
 * Externally, an instance of {@link InstanceResolver} is
 * associated with {@link WSEndpoint}.
 *
 * <h2>Possible Uses</h2>
 * <p>
 * One can use WS-Addressing message properties to
 * decide which instance to deliver a message. This
 * would be an important building block for a stateful
 * web services.
 *
 * <p>
 * One can associate an instance of a service
 * with a specific WS-RM session.
 *
 * @author Kohsuke Kawaguchi
 * @see WSEndpoint#setInstanceResolver(InstanceResolver)
 */
public abstract class InstanceResolver<T> {
    /**
     * Decides which instance of 'T' serves the given request message.
     *
     * <p>
     * This method is called concurrently by multiple threads.
     * It is also on a criticail path that affects the performance.
     * A good implementation should try to avoid any synchronization,
     * and should minimize the amount of work as much as possible.
     *
     * @param request
     *      Always non-null. Represents the request message to be served.
     *      The caller may not consume the {@link Message}.
     *
     * @return
     *      Must return a non-null value.
     */
    public abstract T resolve(Packet request);


    private static final class Singleton<T> extends InstanceResolver<T> {
        private final T singleton;

        public Singleton(T singleton) {
            this.singleton = singleton;
        }

        public T resolve(Packet request) {
            return singleton;
        }
    }

    /**
     * Creates a {@link InstanceResolver} implementation that always
     * returns the specified singleton instance.
     */
    public static <T> InstanceResolver<T> createSingleton(T singleton) {
        assert singleton!=null;
        return new Singleton<T>(singleton);
    }
}
