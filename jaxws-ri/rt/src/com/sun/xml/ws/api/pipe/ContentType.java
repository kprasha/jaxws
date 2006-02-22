package com.sun.xml.ws.api.pipe;

/**
 * A Content-Type transport header that will be returned by {@link Encoder#encode(com.sun.xml.ws.api.message.Packet, java.io.OutputStream)}.
 * It will provide the Content-Type header and also take care of SOAP 1.1 SOAPAction header.
 *
 * @author Vivek Pandey
 */
public interface ContentType {
    /**
     * Gives non-null Content-Type header value.
     */
    public String getContentType();

    /**
     * Gives SOAPAction transport header value. It will be non-null only for SOAP 1.1 messages. In other cases
     * it MUST be null. The SOAPAction transport header should be written out only when its non-null.
     *
     * @return It can be null, in that case SOAPAction header should be written.
     */
    public String getSOAPAction();
}
