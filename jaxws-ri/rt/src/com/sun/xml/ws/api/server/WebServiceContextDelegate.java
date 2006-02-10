package com.sun.xml.ws.api.server;

import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.Pipe;

import javax.xml.ws.WebServiceContext;
import java.security.Principal;

/**
 * This object is set to {@link Packet#webServiceContextDelegate}
 * to serve {@link WebServiceContext} methods for a {@link Packet}.
 *
 * <p>
 * When the user application calls a method on {@link WebServiceContext},
 * the JAX-WS RI goes to the {@link Packet} that represents the request,
 * then check {@link Packet#webServiceContextDelegate}, and forwards
 * the method calls to {@link WebServiceContextDelegate}. 
 *
 * <p>
 * All the methods defined on this interface takes {@link Packet}
 * (whose {@link Packet#webServiceContextDelegate} points to
 * this object), so that a single stateless {@link WebServiceContextDelegate}
 * can be used to serve multiple concurrent {@link Packet}s,
 * if the implementation wishes to do so.
 *
 * <p>
 * (It is also allowed to create one instance of
 * {@link WebServiceContextDelegate} for each packet,
 * and thus effectively ignore the packet parameter.)
 *
 * <p>
 * Attaching this on a {@link Packet} allows {@link Pipe}s to
 * intercept and replace them, if they wish.
 *
 *
 * @author Kohsuke Kawaguchi
 */
public interface WebServiceContextDelegate {
    /**
     * Implements {@link WebServiceContext#getUserPrincipal()}
     * for the given packet.
     *
     * @param request
     *      Always non-null.
     * @see WebServiceContext#getUserPrincipal()
     */
    Principal getUserPrincipal(Packet request);

    /**
     * Implements {@link WebServiceContext#isUserInRole(String)}
     * for the given packet.
     *
     * @param request
     *      Always non-null.
     * @see WebServiceContext#isUserInRole(String)
     */
    boolean isUserInRole(Packet request,String role);
}
