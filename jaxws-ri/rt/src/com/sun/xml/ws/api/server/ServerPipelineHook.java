package com.sun.xml.ws.api.server;

import com.sun.istack.NotNull;
import com.sun.istack.Nullable;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.helper.AbstractFilterPipeImpl;

/**
 * Allow the container (primarily Glassfish) to inject
 * their own pipes into the pipeline.
 *
 * <p>
 * This interface has a rather ad-hoc set of methods, because
 * we didn't want to define an autonomous pipe-assembly process.
 * (We thought this is a smaller evil compared to that.)
 *
 * <p>
 * JAX-WS obtains this through {@link Container#getSPI(Class)}.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class ServerPipelineHook {
    /**
     * Called during the pipeline construction process once to allow a container
     * to register a pipe for monitoring.
     *
     * This pipe will be injected to a point very close to the transport, allowing
     * it to measure the time it takes for processing as well as detecting errors.
     *
     * @param wsdlModel
     *      The created pipeline will be used to serve this port.
     *      Null if the service isn't associated with any port,
     *      and otherwise non-null.
     *
     * @param owner
     *      The created pipeline is used to serve this {@link WSEndpoint}.
     *
     * @param tail
     *      Head of the partially constructed pipeline. If the implementation
     *      wishes to add new pipes, it should do so by extending
     *      {@link AbstractFilterPipeImpl} and making sure that this {@link Pipe}
     *      eventually processes messages.
     *
     * @return
     *      The default implementation just returns <tt>tail</tt>, which means
     *      no additional pipe is inserted. If the implementation adds
     *      new pipes, return the new head pipe.
     */
    public @NotNull Pipe createMonitoringPipe(@Nullable WSDLPort wsdlModel, @NotNull WSEndpoint owner, @NotNull Pipe tail) {
        return tail;
    }
}
