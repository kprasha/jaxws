package com.sun.tools.ws.api;

import org.xml.sax.EntityResolver;

import java.net.URI;

import com.sun.istack.Nullable;
import com.sun.istack.NotNull;

/**
 * Extension point for resolving metadata using wsimport.
 * <p/>
 * wsimport would get a {@link MetaDataResolver} using this factory and from it will resolve all the wsdl/schema metadata.
 *
 * @author Vivek Pandey
 * @see MetaDataResolver#resolve(java.net.URI)
 */
public abstract class MetadataResolverFactory {
    /**
     * Gets a {@link MetaDataResolver}
     * @param location
     * @param resolver
     */
    public abstract @NotNull MetaDataResolver metadataResolver(@NotNull URI location, @Nullable EntityResolver resolver);
}
