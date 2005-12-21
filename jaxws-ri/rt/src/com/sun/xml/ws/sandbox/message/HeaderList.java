package com.sun.xml.ws.sandbox.message;

import com.sun.xml.ws.sandbox.Decoder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A list of {@link Header}s on a {@link Message}.
 *
 * <p>
 * This list can be modified to add headers
 * from outside a {@link Message}, this is necessary
 * since intermediate processing layers often need to
 * put additional headers.
 *
 * <p>
 * Following the SOAP convention, the order among headers
 * are not significant. However, {@link Decoder}s are
 * expected to preserve the order of headers in the input
 * message as much as possible.
 *
 * @see Message#getHeaders()
 */
public final class HeaderList extends ArrayList<Header> {

    /**
     * Creates an empty {@link HeaderList}.
     */
    public HeaderList() {
    }

    /**
     * Copy constructor.
     */
    public HeaderList(List<Header> headers) {
        super(headers);
    }

    /**
     * The number of total headers.
     */
    public int size() {
        return super.size();
    }

    /**
     * Gets the {@link Header} at the specified index.
     */
    public Header get(int index) {
        return super.get(index);
    }

    /**
     * Gets the first {@link Header} of the specified name.
     *
     * @return null
     *      if not found.
     */
    public Header get(String nsUri, String localName) {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets an iteration of headers {@link Header} of the specified name.
     *
     * @return empty iterator
     *      if not found.
     */
    public Iterator<Header> getHeaders(String nsUri, String localName) {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets an iteration of headers {@link Header} in the specified namespace.
     *
     * @return empty iterator
     *      if not found.
     */
    public Iterator<Header> getHeaders(String nsUri) {
        throw new UnsupportedOperationException();
    }

    /**
     * Adds a new {@link Header}.
     *
     * <p>
     * Order doesn't matter in headers, so this method
     * does not make any guarantee as to where the new header
     * is inserted.
     *
     * @return
     *      always true. Don't use the return value.      
     */
    public boolean add(Header header) {
        return super.add(header);
    }
}
