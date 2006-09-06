package com.sun.xml.ws.api.server;

import javax.xml.ws.WebServiceContext;

/**
 * @author Jitendra Kotamraju
 * @author Kohsuke Kawaguchi
 */
public interface AsyncProvider<T> {
    public void invoke(T req, AsyncProviderCallback<T> callback, WebServiceContext ctxt);
}
