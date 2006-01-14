/*
 * Copyright (c) 2006 Your Corporation. All Rights Reserved.
 */
package com.sun.xml.ws.client;

import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.PipelineAssembler;
import com.sun.xml.ws.api.model.RuntimeModel;

public abstract class PipeAssemblerImpl implements PipelineAssembler {

    /**Todo not sure if there will be one for dispatch, one for proxy,
     * one for standalone, one for tango, etc so make abstract for now
     */


    /**
     * Creates a new pipeline.
     * <p/>
     * <p/>
     * When the runtime needs multiple pipelines from the same
     * configuration, it does so by making a {@link com.sun.xml.ws.api.pipe.Pipe#copy(com.sun.xml.ws.api.pipe.PipeCloner) copy}.
     * So this method can assume that every time it's invoked
     * the <tt>model</tt> would be different.
     * (TODO:exact nature of such assumption depends on how we
     * design discovery mechanism. so this might change.)
     *
     * @param model The created pipeline will be used to serve this model.
     *              Always non-null.
     * @return non-null freshly created pipeline.
     * @throws javax.xml.ws.WebServiceException
     *          if there's any configuration error that prevents the
     *          pipeline from being constructed. This exception will be
     *          propagated into the application, so it must have
     *          a descriptive error.
     */
    public abstract Pipe create(RuntimeModel model);

    //Todo: Note: added simply to get Dispatch working with some message

    /**
     * @param dataStore - datastore for Dispatch related info as dispatch may or may not
     *                  have a Runtime model.  TOdo: what is this Object?
     * todo:needs to be added to interface
     * @return pipe - master pipe or clone?
     */
    public abstract Pipe create(Object dataStore);
}
