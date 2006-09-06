package com.sun.xml.ws.api.server;

/**
 * @author Jitendra Kotamraju
 * @author Kohsuke Kawaguchi
 */
public interface AsyncProviderCallback<T> {
    void send(T response);
    void sendError(Throwable t);
}
