package com.sun.xml.ws.client.port;

import com.sun.xml.ws.client.RequestContext;

import javax.xml.ws.Response;
import javax.xml.ws.WebServiceException;

/**
 * {@link MethodHandler} that handles asynchronous invocations through {@link Response}.
 * @author Kohsuke Kawaguchi
 */
final class PollingMethodHandler extends AsyncMethodHandler {

    public PollingMethodHandler(PortInterfaceStub owner, MethodHandler core) {
        super(owner,core);
    }

    public Response<?> invoke(final Object proxy, final Object[] args, RequestContext rc) throws WebServiceException {
        return doInvoke(proxy, args, rc, null);
    }
}
