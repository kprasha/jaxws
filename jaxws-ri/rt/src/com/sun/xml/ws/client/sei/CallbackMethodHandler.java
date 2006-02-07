package com.sun.xml.ws.client.sei;

import javax.xml.ws.AsyncHandler;
import javax.xml.ws.WebServiceException;
import java.util.concurrent.Future;

/**
 * {@link MethodHandler} that uses {@link AsyncHandler}.
 *
 * @author Kohsuke Kawaguchi
 */
final class CallbackMethodHandler extends AsyncMethodHandler {

    /**
     * Position of the argument that takes {@link AsyncHandler}.
     */
    private final int handlerPos;

    public CallbackMethodHandler(SEIStub owner, SyncMethodHandler core, int handlerPos) {
        super(owner,core);
        this.handlerPos = handlerPos;
    }

    public Future<?> invoke(Object proxy, Object[] args) throws WebServiceException {
        // the spec requires the last argument
        final AsyncHandler handler = (AsyncHandler)args[handlerPos];

        return doInvoke(proxy, args, handler);
    }
}
