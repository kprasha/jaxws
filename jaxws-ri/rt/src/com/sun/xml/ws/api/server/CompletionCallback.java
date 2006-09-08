package com.sun.xml.ws.api.server;

import com.sun.xml.ws.api.message.Packet;

/**
 * TODO:javadoc
 * @author Kohsuke Kawaguchi
 */
public interface CompletionCallback {
    /**
     * TODO:javadoc
     */
    void onCompletion(Packet response);
}
