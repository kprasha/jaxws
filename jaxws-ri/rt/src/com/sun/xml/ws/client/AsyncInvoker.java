package com.sun.xml.ws.client;

import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.Fiber.CompletionCallback;
import com.sun.xml.ws.api.pipe.Tube;

/**
 * Invokes {@link Tube}line asynchronously for the client's async API(for e.g.: Dispatch#invokeAsync}
 * The concrete classes need to call {@link Stub#processAsync(Packet, RequestContext, CompletionCallback)} in
 * run() method.
 *
 * @author Jitendra Kotamraju
 */
public abstract class AsyncInvoker implements Runnable {
    /**
     * Because of the object instantiation order,
     * we can't take this as a constructor parameter.
     */
    protected AsyncResponseImpl responseImpl;

    public void setReceiver(AsyncResponseImpl responseImpl) {
        this.responseImpl = responseImpl;
    }

}
