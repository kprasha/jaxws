package com.sun.xml.ws.client.port;

import com.sun.xml.ws.client.RequestContext;

import javax.xml.ws.WebServiceException;

/**
 * Handles an invocation of a method.
 *
 * <p>
 * Each instance of {@link MethodHandler} has an implicit knowledge of
 * a particular method that it handles.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class MethodHandler {

    protected final PortInterfaceStub owner;

    protected MethodHandler(PortInterfaceStub owner) {
        this.owner = owner;
    }

    /**
     * Performs the method invocation.
     *
     * @param proxy
     *      The proxy object exposed to the user. Must not be null.
     * @param args
     *      The method invocation arguments. To handle asynchroonus method invocations
     *      without array reallocation, this aray is allowed to be longer than the
     *      actual number of arguments to the method. Additional array space should be
     *      simply ignored.
     *
     * @param rc
     *      This {@link RequestContext} is used for invoking this method.
     *      We take this as a separate parameter because of the async invocation
     *      handling, which requires a separate copy.
     * @return
     *      a return value from the method invocation. may be null.
     */
    public abstract Object invoke(Object proxy, Object[] args, RequestContext rc) throws WebServiceException, Throwable;
}
