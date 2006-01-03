package com.sun.xml.ws.client.port;

import javax.xml.ws.Response;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Callable;
import java.util.Map;

/**
 * @author Kohsuke Kawaguchi
 */
final class ResponseImpl<T> extends FutureTask<T> implements Response<T> {
    protected ResponseImpl(Callable<T> callable) {
        super(callable);
    }

    public Map<String, Object> getContext() {
        // TODO
        throw new UnsupportedOperationException();
    }
}
