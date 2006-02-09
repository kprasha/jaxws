package com.sun.xml.ws.api.server;

import com.sun.xml.ws.api.WSEndpoint;

/**
 * Resolves relative references among {@link SDDocument}s.
 *
 * <p>
 * This interface is implemented by the caller of
 * {@link SDDocument#writeTo)} method so
 * that the {@link SDDocument} can correctly produce references
 * to other documents.
 *
 * <p>
 * This mechanism allows the user of {@link WSEndpoint} to
 * assign logical URLs to each {@link SDDocument} (which is often
 * necessarily done in a transport-dependent way), and then
 * serve description documents.
 *
 * @author Kohsuke Kawaguchi
 */
public interface DocumentAddressResolver {
    /**
     * Produces a relative reference from one document to another.
     *
     * @param current
     *      The document that is being generated. Always non-null.
     * @param referenced
     *      The document that is referenced. Always non-null.
     * @return
     *      The reference to be put inside {@code current} to refer to
     *      {@code referenced}. This can be a relative URL as well as
     *      an absolute. Must not be null.
     */
    String getRelativeAddressFor(SDDocument current, SDDocument referenced);
}
