package com.sun.xml.ws.sandbox.server;

import javax.xml.stream.XMLStreamWriter;
import java.io.OutputStream;

/**
 * Represents an individual document that forms a {@link ServiceDefinition}.
 *
 * <pre>
 * TODO:
 *      how does those documents refer to each other?
 * </pre>
 *
 * @author Jitu
 */
public interface SDDocument {

    //public enum Kind { WSDL, SCHEMA, OTHER }

    /**
     * Writes the document to the given {@link OutputStream}.
     *
     * <p>
     * Since {@link ServiceDefinition} doesn't know which endpoint address
     * {@link Adapter} is serving to, (and often it serves multiple URLs
     * simultaneously), this method takes the address as a parameter,
     * so that it can produce the corret address information in the generated WSDL.
     *
     * @param endpointAddress
     *      The URL that represents the endpoint address,
     *      such as "http://ws.sun.com/foo/". Must not be null.
     * @param os
     *      The {@link OutputStream} that receives the generated document.
     */
    void writeTo(String endpointAddress, OutputStream os);

    /**
     * Writes the document to the given {@link XMLStreamWriter}.
     *
     * <p>
     * The same as {@link #writeTo(String, OutputStream)} except
     * it writes to an {@link XMLStreamWriter}.
     *
     * <p>
     * The implementation must not call {@link XMLStreamWriter#writeStartDocument()}
     * nor {@link XMLStreamWriter#writeEndDocument()}. Those are the caller's
     * responsibility.
     */
    void writeTo(String endpointAddress, XMLStreamWriter out);

    /**
     * Gets the MIME content-type of this document.
     *
     * TODO: do we need this? Isn't it always "application/xml; charset=UTF-8"?
     *
     * @return
     *      always non-null string, such as <tt>"application/xml; charset=UTF-8"</tt>.
     */
    String getContentType();
}
