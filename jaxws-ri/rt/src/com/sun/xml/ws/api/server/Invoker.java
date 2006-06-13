package com.sun.xml.ws.api.server;

import com.sun.istack.NotNull;
import com.sun.xml.ws.api.message.Packet;

import javax.xml.ws.WebServiceContext;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/**
 * Hides the detail of calling into application endpoint implementation.
 *
 * @author Jitendra Kotamraju
 * @author Kohsuke Kawaguchi
 */
public abstract class Invoker {
    /**
     * Called by {@link WSEndpoint} when it's set up.
     *
     * <p>
     * This is an opportunity for {@link InstanceResolver}
     * to do a endpoint-specific initialization process.
     *
     * @param wsc
     *      The {@link WebServiceContext} instance to be injected
     *      to the user instances (assuming {@link InstanceResolver}
     */
    public void start(@NotNull WebServiceContext wsc) {}

    /**
     * Called by {@link WSEndpoint}
     * when {@link WSEndpoint#dispose()} is called.
     *
     * This allows {@link InstanceResolver} to do final clean up.
     *
     * <p>
     * This method is guaranteed to be only called once by {@link WSEndpoint}.
     */
    public void dispose() {}

    /**
     *
     */
    public abstract Object invoke( @NotNull Packet p, @NotNull Method m, @NotNull Object... args ) throws InvocationTargetException, IllegalAccessException;
}
