package com.sun.xml.ws.util;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * {@link Future} implementation that obtains an already available value.
 *
 * @author Kohsuke Kawaguchi
 */
public class CompletedFuture<T> implements Future<T> {
    private final T v;

    public CompletedFuture(T v) {
        this.v = v;
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    public boolean isCancelled() {
        return false;
    }

    public boolean isDone() {
        return true;
    }

    public T get() {
        return v;
    }

    public T get(long timeout, TimeUnit unit) {
        return get();
    }
}
