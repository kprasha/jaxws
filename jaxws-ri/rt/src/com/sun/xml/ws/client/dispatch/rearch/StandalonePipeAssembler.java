/*
 * Copyright (c) 2006 Your Corporation. All Rights Reserved.
 */
package com.sun.xml.ws.client.dispatch.rearch;

import com.sun.xml.ws.client.PipeAssemblerImpl;
import com.sun.xml.ws.sandbox.impl.TestDecoderImpl;
import com.sun.xml.ws.sandbox.impl.TestEncoderImpl;
import com.sun.xml.ws.sandbox.pipe.Pipe;
import com.sun.xml.ws.sandbox.api.model.RuntimeModel;
import com.sun.xml.ws.transport.http.client.HttpTransportPipe;

public class StandalonePipeAssembler extends PipeAssemblerImpl {
    /**
     * Creates a new pipeline.
     * <p/>
     * <p/>
     * When the runtime needs multiple pipelines from the same
     * configuration, it does so by making a {@link com.sun.xml.ws.sandbox.pipe.Pipe#copy(com.sun.xml.ws.sandbox.pipe.PipeCloner) copy}.
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
    public Pipe create(RuntimeModel model) {
        throw new UnsupportedOperationException();
    }

    /**
     * @param dataStore - datastore for Dispatch related info as dispatch may or may not
     *                  have a Runtime model.  TOdo: what is this Object?
     * @return pipe - master pipe or clone?
     */
    //todo:temp here for dispatch so can get something working
    //todo: for dispatch what is this datastore?
    //todo: currently using test encoder/decoder
    public Pipe create(Object dataStore) {
        return new HttpTransportPipe(TestEncoderImpl.INSTANCE, TestDecoderImpl.INSTANCE11);
    }
}
