package com.sun.xml.ws.util;

import com.sun.xml.bind.api.JAXBRIContext;
import com.sun.xml.ws.api.pipe.Pipe;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * General-purpose object pool.
 *
 * <p>
 * In many parts of the runtime, we need to pool instances of objects that
 * are expensive to create (such as JAXB objects, StAX parsers, {@link Pipe} instances.)
 *
 * <p>
 * This class provides a default implementation of such a pool.
 *
 * TODO: improve the implementation
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class Pool<T> {
    private final ConcurrentLinkedQueue<T> queue = new ConcurrentLinkedQueue<T>();

    /**
     * Gets a new object from the pool.
     *
     * <p>
     * If no object is available in the pool, this method creates a new one.
     *
     * @return
     *      always non-null.
     */
    public final T take() {
        T t = queue.poll();
        if(t==null)
            return create();
        return t;
    }

    /**
     * Returns an object back to the pool.
     */
    public final void recycle(T t) {
        queue.offer(t);
    }

    /**
     * Creates a new instance of object.
     *
     * <p>
     * This method is used when someone wants to
     * {@link #take() take} an object from an empty pool.
     *
     * <p>
     * Also note that multiple threads may call this method
     * concurrently.
     */
    protected abstract T create();


    /**
     * JAXB {@link javax.xml.bind.Marshaller} pool.
     */
    public static final class Marshaller extends Pool<javax.xml.bind.Marshaller> {
        private final JAXBContext context;

        public Marshaller(JAXBContext context) {
            this.context = context;
        }

        protected javax.xml.bind.Marshaller create() {
            try {
                return context.createMarshaller();
            } catch (JAXBException e) {
                // impossible
                throw new AssertionError(e);
            }
        }
    }

    /**
     * JAXB {@link com.sun.xml.bind.api.BridgeContext} pool.
     */
    public static final class BridgeContext extends Pool<com.sun.xml.bind.api.BridgeContext> {
        private final JAXBRIContext context;

        public BridgeContext(JAXBRIContext context) {
            this.context = context;
        }

        protected com.sun.xml.bind.api.BridgeContext create() {
            return context.createBridgeContext();
        }
    }
}
