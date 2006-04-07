package com.sun.tools.ws.api;

import com.sun.istack.NotNull;

import javax.xml.transform.Source;
import java.util.List;

/**
 * Abstraction over WSDL and Schema metadata
 *
 * @author Vivek Pandey
 */
public abstract class ServiceDescriptor {
    /**
     * Gives list of wsdls
     * @return List of WSDL documents as {@link Source}
     */
    public abstract @NotNull List<? extends Source> getWSDLs();

    /**
     * Gives list of schemas
     * @return List of XML schema documents as {@link Source}
     */
    public abstract @NotNull List<? extends Source> getSchemas();
}
