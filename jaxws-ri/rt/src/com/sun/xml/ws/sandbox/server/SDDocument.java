package com.sun.xml.ws.sandbox.server;

import com.sun.xml.ws.api.WSEndpoint;

import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.ws.WebServiceException;
import javax.xml.namespace.QName;
import java.io.OutputStream;
import java.io.IOException;
import java.net.URL;

/**
 * Represents an individual document that forms a {@link ServiceDefinition}.
 *
 * <pre>
 * TODO:
 *      how does those documents refer to each other?
 * TODO:
 *      rename!
 * </pre>
 *
 * @author Jitu
 */
public interface SDDocument {

    /**
     * Gets the root tag name of this document.
     *
     * <p>
     * This can be used to identify a kind of document quickly
     * (such as schema, WSDL, ...)
     *
     * @return
     *      always non-null.
     */
    QName getRootName();

    /**
     * Returns true if this document is WSDL.
     */
    boolean isWSDL();

    /**
     * Returns true if this document is schema.
     */
    boolean isSchema();

    /**
     * Gets the system ID of the document where it's taken from.
     */
    URL getURL();

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
     * @param resolver
     *      Used to resolve relative references among documents.
     * @param os
     *      The {@link OutputStream} that receives the generated document.
     *
     * @throws IOException
     *      if there was a failure reported from the {@link OutputStream}.
     */
    void writeTo(String endpointAddress, DocumentAddressResolver resolver, OutputStream os) throws IOException;

    /**
     * Writes the document to the given {@link XMLStreamWriter}.
     *
     * <p>
     * The same as {@link #writeTo(String,DocumentAddressResolver,OutputStream)} except
     * it writes to an {@link XMLStreamWriter}.
     *
     * <p>
     * The implementation must not call {@link XMLStreamWriter#writeStartDocument()}
     * nor {@link XMLStreamWriter#writeEndDocument()}. Those are the caller's
     * responsibility.
     *
     * @throws XMLStreamException
     *      if the {@link XMLStreamWriter} reports an error.
     */
    void writeTo(String endpointAddress, DocumentAddressResolver resolver, XMLStreamWriter out) throws XMLStreamException, IOException;

    /**
     * {@link SDDocument} that represents an XML Schema.
     */
    interface Schema extends SDDocument {
        /**
         * Gets the target namepsace of this schema.
         */
        String getTargetNamespace();
    }

    /**
     * {@link SDDocument} that represents a WSDL.
     */
    interface WSDL extends SDDocument {
        /**
         * Gets the target namepsace of this schema.
         */
        String getTargetNamespace();

        /**
         * This WSDL has a portType definition
         * that matches what {@link WSEndpoint} is serving.
         *
         * TODO: does this info needs to be exposed?
         */
        boolean hasPortType();

        /**
         * This WSDL has a service definition
         * that matches the {@link WSEndpoint}.
         *
         * TODO: does this info need to be exposed?
         */
        boolean hasService();
    }
}
