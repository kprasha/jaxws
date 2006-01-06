package com.sun.xml.ws.sandbox.pipe;

import com.sun.xml.ws.model.RuntimeModel;

import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;
import javax.xml.namespace.QName;
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
 * @author Kohsuke Kawaguchi
 */
public interface PipelineAssembler {
    /**
     * Creates a new pipeline.
     *
     * <p>
     * When the runtime needs multiple pipelines from the same
     * configuration, it does so by making a {@link Pipe#copy(PipeCloner) copy}.
     * So this method can assume that every time it's invoked
     * the <tt>model</tt> would be different.
     * (TODO:exact nature of such assumption depends on how we
     * design discovery mechanism. so this might change.) 
     *
     * @param model
     *      The created pipeline will be used to serve this model.
     *      Always non-null.
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
    Pipe create(RuntimeModel model);
}
