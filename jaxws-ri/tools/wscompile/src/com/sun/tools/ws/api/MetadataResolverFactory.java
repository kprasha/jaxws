package com.sun.tools.ws.api;

import org.xml.sax.EntityResolver;

import java.net.URI;

/**
 * Extension point for resolving metadata using wsimport.
 * <p/>
 * wsimport would get a {@link MetadataResolver} using this factory and from it will resolve all the wsdl/schema metadata.
 *
 * @author Vivek Pandey
 * @see MetadataResolver#resolve(java.net.URI)
 */
public abstract class MetadataResolverFactory {
    /**
     * get a {@link 
     * @param location
     * @param resolver
     * @return
     */
    public abstract MetadataResolver metadataResolver(URI location, EntityResolver resolver);
}
