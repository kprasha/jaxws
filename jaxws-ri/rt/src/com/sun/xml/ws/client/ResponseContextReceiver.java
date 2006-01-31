package com.sun.xml.ws.client;

/**
 * Receives {@link ResponseContext} at the end of
 * the message invocation.
 *
 * @author Kohsuke Kawaguchi
 */
public interface ResponseContextReceiver {
    /**
     * Called upon the completion of the invocation
     * to set a {@link ResponseContext}.
     */
    void setResponseContext(ResponseContext rc);
}
