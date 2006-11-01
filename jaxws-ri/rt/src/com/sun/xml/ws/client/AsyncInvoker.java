package com.sun.xml.ws.client;

import com.sun.xml.ws.util.CompletedFuture;

import javax.xml.ws.Response;
import javax.xml.ws.AsyncHandler;
import java.util.concurrent.FutureTask;
import java.util.Map;

/**
 * {@link javax.xml.ws.Response} implementation.
 *
 * @author Jitendra Kotamraju
 */
public abstract class AsyncInvoker<T> implements Runnable {
    /**
     * Because of the object instantiation order,
     * we can't take this as a constructor parameter.
     */
    protected AsyncResponseImpl responseImpl;

    public void setReceiver(AsyncResponseImpl responseImpl) {
        this.responseImpl = responseImpl;
    }

}
