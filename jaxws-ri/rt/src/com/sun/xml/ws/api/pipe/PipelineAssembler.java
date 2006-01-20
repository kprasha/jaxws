package com.sun.xml.ws.api.pipe;

import com.sun.xml.ws.api.WSEndpoint;
import com.sun.xml.ws.api.WSService;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.model.RuntimeModel;
import com.sun.xml.ws.api.model.wsdl.WSDLBinding;

import javax.xml.namespace.QName;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.Provider;
import java.net.URL;

/**
 * Creates a pipeline.
 *
 * <p>
 * This pluggability layer enables the upper layer to
 * control exactly how the pipeline is composed.
 *
 * <p>
 * JAX-WS is going to have its own default implementation
 * when used all by itself, but it can be substituted by
 * other implementations.
 *
 * <p>
 * TODO: define the exact mechanism to locate the implementation
 * of this class. Perhaps we might define a proprietary entry point
 * that works like {@link Service#create(URL, QName)} but with
 * additional parameter that takes an instance of {@link PipelineAssembler}.
 *
 * <p>
 * TODO: the JAX-WS team felt that no {@link Pipe} should be relying
 * on the {@link RuntimeModel}, so it is no longer given to the assembler.
 * Talk to us if you need it.
 *
 *
 * @author Kohsuke Kawaguchi
 */
public interface PipelineAssembler {
    /**
     * Creates a new pipeline for clients.
     *
     * <p>
     * When a JAX-WS client creates a proxy or a {@link Dispatch} from
     * a {@link Service}, JAX-WS runtime internally uses this method
     * to create a new pipeline as a part of the initilization.
     *
     * @param wsdlModel
     *      The created pipeline will be used to serve this port binding.
     *      Null if the service isn't associated with any port,
     *      and otherwise non-null.
     *
     * @param rootOwner
     *      The pipeline is created for this {@link WSService}.
     *      Always non-null. (To be precise, the newly created pipeline
     *      is owned by a proxy or a dispatch created from thsi {@link WSService}.)
     *
     * @return
     *      non-null freshly created pipeline.
     *
     * @throws WebServiceException
     *      if there's any configuration error that prevents the
     *      pipeline from being constructed. This exception will be
     *      propagated into the application, so it must have
     *      a descriptive error.
     */
    Pipe createClient(WSDLBinding wsdlModel, WSService rootOwner);

    /**
     * Creates a new pipeline for servers.
     *
     * <p>
     * When a JAX-WS server deploys a new endpoint, it internally
     * uses this method to create a new pipeline as a part of the
     * initialization.
     *
     * <p>
     * Note that this method is called only once to set up a
     * 'master pipeline', and it gets {@link Pipe#copy(PipeCloner) copied}
     * from it.
     *
     * @param wsdlModel
     *      The created pipeline will be used to serve this port binding.
     *      Null if the service isn't associated with any port,
     *      and otherwise non-null.
     *
     * @param owner
     *      The created pipeline is used to serve this {@link WSEndpoint}.
     *      Specifically, its {@link WSBinding} should be of interest to
     *      many {@link Pipe}s. Always non-null.
     *
     * @param terminal
     *      The last {@link Pipe} in the pipeline.
     *      Always non-null.
     *      The assembler
     *      is expected to put additional {@link Pipe}s in front
     *      of it.
     *
     *      <p>
     *      (Just to give you the idea how this is used, normally
     *      the terminal pipe is the one that invokes the user application
     *      or {@link Provider}.)
     */
    Pipe createServer(WSDLBinding wsdlModel, WSEndpoint owner, Pipe terminal);
}
