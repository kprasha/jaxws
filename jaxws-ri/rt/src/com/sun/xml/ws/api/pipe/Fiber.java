package com.sun.xml.ws.api.pipe;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.ArrayList;
import com.sun.xml.ws.api.message.Packet;

/**
 * User-level thread used to implement CPS.
 *
 * TODO: doc improvement!
 *
 * <h2>Context ClassLoader</h2>
 * <p>
 * Just like thread, a fiber has a context class loader (CCL.) A fiber's CCL
 * becomes the thread's CCL when it's executing the fiber. The original CCL
 * of the thread will be restored when the thread leaves the fiber execution.
 *
 * @author Kohsuke Kawaguchi
 * @author Jitendra Kotamraju
 */
public class Fiber implements Runnable {
    /**
     * {@link Tube}s whose {@link Tube#processResponse(Packet)} method needs
     * to be invoked on the way back.
     */
    private Tube[] conts = new Tube[16];
    private int contsSize;

    /**
     * If this field is non-null, the next instruction to execute is
     * to call its {@link Tube#processRequest(Packet)}. Otherwise
     * the instruction is to call {@link #conts}.
     */
    private Tube next;

    private Packet packet;

    public final Engine owner;

    /**
     * Is this thread suspended? 0=not suspended, 1=suspended.
     *
     * <p>
     * Logically this is just a boolean, but we need to prepare for the case
     * where the thread is {@link #resume(Packet) resumed} before we get to the {@link #suspend()}.
     * This happens when things happen in the following order:
     *
     * <ol>
     *  <li>Tube decides that the fiber needs to be suspended to wait for the external event.
     *  <li>Tube hooks up fiber with some external mechanism (like NIO channel selector)
     *  <li>Tube returns with {@link NextAction#suspend()}.
     *  <li>"External mechanism" becomes signal state and invokes {@link Fiber#resume(Packet)}
     *      to wake up fiber
     *  <li>{@link Fiber#doRun} invokes {@link Fiber#suspend()}.
     * </ol>
     *
     * <p>
     * Using int, this will work OK because {@link #suspendedCount} becomes -1 when
     * {@link #resume(Packet)} occurs before {@link #suspend()}.
     *
     * <p>
     * Increment and decrement is guarded by 'this' object.
     */
    private volatile int suspendedCount = 0;

    /**
     * Is this fiber completed?
     */
    private boolean completed;

    /**
     * Is this {@link Fiber} currently running in the synchronous mode?
     */
    private boolean synchronous;

    private boolean interrupted;

    private final int id;

    /**
     * Active {@link FiberContextSwitchInterceptor}s for this fiber.
     */
    private List<FiberContextSwitchInterceptor> interceptors;

    /**
     * Not null when {@link #interceptors} is not null.
     */
    private InterceptorHandler interceptorHandler;

    /**
     * This flag is set to true when a new interceptor is added.
     *
     * When that happens, we need to first exit the current interceptors
     * and then reenter them, so that the newly added interceptors start
     * taking effect. This flag is used to control that flow.
     */
    private boolean needsToReenter;

    /**
     * Fiber's context {@link ClassLoader}. Can be null.
     */
    private ClassLoader contextClassLoader;

    Fiber(Engine engine) {
        this.owner = engine;
        if(DEBUG) {
            id = iotaGen.incrementAndGet();
            System.out.println(getName()+" created");
        } else {
            id = -1;
        }

        // if this is run from another fiber, then we naturally inherit its context classloader,
        // so this code works for fiber->fiber inheritance just fine.
        contextClassLoader = Thread.currentThread().getContextClassLoader();
    }

    public void start(Tube startPoint, Packet packet) {
        next = startPoint;
        this.packet = packet;
        owner.addRunnable(this);
    }

    /**
     * Application will call this method when this continuation becomes runnable again.
     */
    public synchronized void resume(Packet response) {
        if(DEBUG)
            System.out.println(getName()+" resumed");
        packet = response;
        if( --suspendedCount == 0 ) {
            if(synchronous) {
                notifyAll();
            } else {
                owner.addRunnable(this);
            }
        }
    }


    /**
     * Suspends this fiber's execution until the resume method is invoked.
     *
     * The call returns immediately, and when the fiber is resumed
     * the execution picks up from the last scheduled continuation.
     */
    private synchronized void suspend() {
        if(DEBUG)
            System.out.println(getName()+" suspended\n");
        suspendedCount++;
    }

    /**
     * Adds a new {@link FiberContextSwitchInterceptor} to this fiber.
     * TODO: doc improvement
     */
    public void addInterceptor(FiberContextSwitchInterceptor interceptor) {
        if(interceptors ==null) {
            interceptors = new ArrayList<FiberContextSwitchInterceptor>();
            interceptorHandler = new InterceptorHandler();
        }
        interceptors.add(interceptor);
        needsToReenter = true;
    }

    /**
     * TODO: doc improvement
     */
    public boolean removeInterceptor(FiberContextSwitchInterceptor interceptor) {
        if(interceptors !=null && interceptors.remove(interceptor)) {
            needsToReenter = true;
            return true;
        }
        return false;
    }

    /**
     * Gets the context {@link ClassLoader} of this fiber.
     */
    public ClassLoader getContextClassLoader() {
        return contextClassLoader;
    }

    public ClassLoader setContextClassLoader(ClassLoader contextClassLoader) {
        ClassLoader r = this.contextClassLoader;
        this.contextClassLoader = contextClassLoader;
        return r;
    }

    /**
     * Not to be called from application. This is an implementation detail
     * of {@link Fiber}.
     */
    @Deprecated
    public void run() {
        assert !synchronous;
        next = doRun(next);
        completionCheck();
    }

    /**
     * Runs the fiber synchronously, starting from the scheduled
     * {@link Tube}.
     *
     * @return
     *      The response packet.
     */
    public synchronized Packet runSync(Tube startPoint, Packet packet) {
        // save the current continuation, so that we return runSync() without executing them.
        final Tube[] oldCont = conts;
        final int oldContSize = contsSize;

        if(oldContSize>0) {
            conts = new Tube[16];
            contsSize=0;
        }

        try {
            synchronous = true;
            this.packet = packet;
            doRun(startPoint);
            return this.packet;
        } finally {
            conts = oldCont;
            contsSize = oldContSize;
            synchronous = false;
            if(interrupted) {
                Thread.currentThread().interrupt();
                interrupted = false;
            }
            completionCheck();
        }
    }

    private synchronized void completionCheck() {
        if(contsSize==0) {
            if(DEBUG)
                System.out.println(getName()+" completed\n");
            completed = true;
            notifyAll();
        }
    }

    /**
     * Blocks until the fiber completes.
     */
    public synchronized void join() throws InterruptedException {
        while(!completed)
            wait();
    }

    /**
     * Invokes all registered {@link InterceptorHandler}s and then call into
     * {@link Fiber#_doRun(Tube)}.
     */
    private class InterceptorHandler implements FiberContextSwitchInterceptor.Work<Tube,Tube> {
        /**
         * Index in {@link Fiber#interceptors} to invoke next.
         */
        private int idx;

        /**
         * Initiate the interception, and eventually invokes {@link Fiber#_doRun(Tube)}.
         */
        Tube invoke(Tube next) {
            idx=0;
            return execute(next);
        }

        public Tube execute(Tube next) {
            if(idx==interceptors.size()) {
                return _doRun(next);
            } else {
                FiberContextSwitchInterceptor interceptor = interceptors.get(idx++);
                return interceptor.execute(Fiber.this,next,this);
            }
        }
    }

    /**
     * Executes the fiber as much as possible.
     *
     * @param next
     *      The next tube whose {@link Tube#processRequest(Packet)} is to be invoked. If null,
     *      that means we'll just call {@link Tube#processResponse(Packet)} on the continuation.
     *
     * @return
     *      If non-null, the next time execution resumes, it should resume from calling
     *      the {@link Tube#processRequest(Packet)}. Otherwise it means just finishing up
     *      the continuation.
     */
    @SuppressWarnings({"LoopStatementThatDoesntLoop"}) // IntelliJ reports this bogus error
    private Tube doRun(Tube next) {
        Thread currentThread = Thread.currentThread();

        if(DEBUG)
            System.out.println(getName()+" running by "+currentThread.getName());

        ClassLoader old = currentThread.getContextClassLoader();
        currentThread.setContextClassLoader(contextClassLoader);
        try {
            do {
                needsToReenter = false;

                // if interceptors are set, go through the interceptors.
                if(interceptorHandler ==null)
                    next = _doRun(next);
                else
                    next = interceptorHandler.invoke(next);
            } while(needsToReenter);

            return next;
        } finally {
            currentThread.setContextClassLoader(old);
        }
    }

    /**
     * To be invoked from {@link #doRun(Tube)}.
     */
    private Tube _doRun(Tube next) {
        final Fiber old = CURRENT_FIBER.get();
        CURRENT_FIBER.set(this);

        try {
            while(!isBlocking() && !needsToReenter) {
                try {
                    NextAction na;
                    Tube last;

                    if(next!=null) {
                        na = next.processRequest(packet);
                        last = next;
                    } else {
                        if(contsSize==0) {
                            // nothing else to execute. we are done.
                            return null;
                        }
                        last = popCont();
                        na = last.processResponse(packet);
                    }

                    packet = na.packet;

                    switch(na.kind) {
                    case NextAction.INVOKE:
                        pushCont(last);
                        // fall through next
                    case NextAction.INVOKE_AND_FORGET:
                        next = na.next;
                        break;
                    case NextAction.RETURN:
                        next = null;
                        break;
                    case NextAction.SUSPEND:
                        pushCont(last);
                        next = null;
                        suspend();
                        break;
                    default:
                        throw new AssertionError();
                    }
                } catch (Throwable t) {
                    // TODO fix it
                    packet = null;
                    //packet = new Packet(t);
                }
            }
            // there's nothing we can execute right away.
            // we'll be back when this fiber is resumed.
            return next;
        } finally {
            CURRENT_FIBER.set(old);
        }
    }

    private void pushCont(Tube tube) {
        conts[contsSize++] = tube;

        // expand if needed
        int len = conts.length;
        if(contsSize==len) {
            Tube[] newBuf = new Tube[len*2];
            System.arraycopy(conts,0,newBuf,0,len);
            conts = newBuf;
        }
    }

    private Tube popCont() {
        return conts[--contsSize];
    }

    /**
     * Returns true if the fiber needs to block its execution.
     */
    // TODO: synchronization on synchronous case is wrong.
    private boolean isBlocking() {
        if(synchronous) {
            while(suspendedCount==1)
                try {
                    System.out.println(getName()+" is blocking thread "+Thread.currentThread().getName());
                    wait();
                } catch (InterruptedException e) {
                    // remember that we are interrupted, but don't respond to it
                    // right away. This behavior is in line with what happens
                    // when you are actually running the whole thing synchronously.
                    interrupted = true;
                }
            return false;
        }
        else
            return suspendedCount==1;
    }

    private String getName() {
        return "fiber"+id;
    }

    public static Fiber current() {
        return CURRENT_FIBER.get();
    }

    /**
     * Creates a new {@link Fiber} as a sibling of the current fiber.
     */
    public static Fiber create() {
        Fiber fiber = current();
        if(fiber==null)
            throw new IllegalStateException("Can be only used from fibers");
        return fiber.owner.createFiber();
    }

    private static final ThreadLocal<Fiber> CURRENT_FIBER = new ThreadLocal<Fiber>();

    private static final AtomicInteger iotaGen = new AtomicInteger();

    /**
     * Set to false to disable debug diagnostis (used for benchmark.)
     */
    public static boolean DEBUG = true;
}
