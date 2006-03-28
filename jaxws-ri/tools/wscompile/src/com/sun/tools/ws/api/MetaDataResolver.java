package com.sun.tools.ws.api;

import com.sun.istack.NotNull;
import com.sun.istack.Nullable;

import java.net.URI;

/**
 * Resolves metadata such as WSDL/schema. This serves as extensibile plugin point which a wsdl parser can use to
 * get the metadata from an endpoint. Implementor of this class must provide a zero argument constructor so that
 * it can be loaded during service lookup mechanism.
 *
 * @author Vivek Pandey
 */
public abstract class MetaDataResolver {
    /**
     * Gives {@link ServiceDescriptor} resolved from the given location.
     *
     * TODO: Does this method need to propogate errors?
     *
     * @param location metadata location
     * @return {@link ServiceDescriptor} resolved from the location. It may be null in the cases when MetaDataResolver
     *         can get the metada associated with the metadata loction.
     */
    public abstract @Nullable ServiceDescriptor resolve(@NotNull URI location);
}
