package com.sun.xml.ws.client.port;

import com.sun.xml.ws.client.RequestContext;

import javax.xml.ws.Response;
import javax.xml.ws.WebServiceException;

/**
 * {@link MethodHandler} that handles asynchronous invocations through {@link Response}.
 * @author Kohsuke Kawaguchi
 */
final class PollingMethodHandler extends AsyncMethodHandler {

    public PollingMethodHandler(PortInterfaceStub owner, SyncMethodHandler core) {
        super(owner,core);
    }

    public Response<?> invoke(Object proxy, Object[] args) throws WebServiceException {
        return doInvoke(proxy,args,null);
    }
}
