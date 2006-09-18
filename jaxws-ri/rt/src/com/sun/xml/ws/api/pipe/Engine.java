package com.sun.xml.ws.api.pipe;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import com.sun.xml.ws.api.message.Packet;

/**
 * Collection of {@link Fiber}s.
 * Owns an {@link Executor} to run them.
 *
 * @author Kohsuke Kawaguchi
 * @author Jitendra Kotamraju
 */
public class Engine {
    private volatile Executor threadPool;

    public Engine(Executor threadPool) {
        this.threadPool = threadPool;
    }

    public Engine() {
    }

    void addRunnable(Fiber fiber) {
        if(threadPool==null) {
            synchronized(this) {
                threadPool = Executors.newFixedThreadPool(3);
            }
        }
        threadPool.execute(fiber);
    }

    /**
     * Creates a new fiber in a suspended state.
     *
     * <p>
     * To start the returned fiber, call {@link Fiber#start(Tube,com.sun.xml.ws.api.message.Packet)}.
     * It will start executing the given {@link Tube} with the given {@link Packet}.
     */
    public Fiber createFiber() {
        return new Fiber(this);
    }
}
