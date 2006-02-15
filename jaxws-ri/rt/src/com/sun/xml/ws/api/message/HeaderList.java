/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 *
 * You can obtain a copy of the license at
 * https://jwsdp.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * HEADER in each file and include the License file at
 * https://jwsdp.dev.java.net/CDDLv1.0.html  If applicable,
 * add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your
 * own identifying information: Portions Copyright [yyyy]
 * [name of copyright owner]
 */
package com.sun.xml.ws.api.message;

import com.sun.xml.ws.api.pipe.Decoder;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

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
     * @param nsUri
     * @return null
     *      if not found.
     */
    public Header get(String nsUri, String localName) {
        int len = size();
        for( int i=0; i<len; i++ ) {
            Header h = get(i);
            if(h.getLocalPart().equals(localName) && h.getNamespaceURI().equals(nsUri))
                return h;
        }
        return null;
    }

    /**
     * Gets the first {@link Header} of the specified name.
     *
     * @return null
     *      if not found.
     */
    public final Header get(QName name) {
        return get(name.getNamespaceURI(),name.getLocalPart());
    }

    /**
     * Gets all the {@link Header}s of the specified name,
     * including duplicates (if any.)
     *
     * @return empty iterator
     *      if not found, but never null.
     */
    public Iterator<Header> getHeaders(final String nsUri, final String localName) {
        return new Iterator<Header>() {
            int idx = 0;
            Header next;
            public boolean hasNext() {
                if(next==null)
                    fetch();
                return next!=null;
            }

            public Header next() {
                if(next==null) {
                    fetch();
                    if(next==null)
                        throw new NoSuchElementException();
                }

                Header r = next;
                next = null;
                return r;
            }

            private void fetch() {
                while(idx<size()) {
                    Header h = get(idx++);
                    if(h.getLocalPart().equals(localName) && h.getNamespaceURI().equals(nsUri)) {
                        next = h;
                        break;
                    }
                }
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Gets an iteration of headers {@link Header} in the specified namespace,
     * including duplicates (if any.)
     *
     * @return empty iterator
     *      if not found. But never null.
     */
    public Iterator<Header> getHeaders(final String nsUri) {
        return new Iterator<Header>() {
            int idx = 0;
            Header next;
            public boolean hasNext() {
                if(next==null)
                    fetch();
                return next!=null;
            }

            public Header next() {
                if(next==null) {
                    fetch();
                    if(next==null)
                        throw new NoSuchElementException();
                }

                Header r = next;
                next = null;
                return r;
            }

            private void fetch() {
                while(idx<size()) {
                    Header h = get(idx++);
                    if(h.getNamespaceURI().equals(nsUri)) {
                        next = h;
                        break;
                    }
                }
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
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

    /**
     * Creates a copy.
     *
     * This handles null {@link HeaderList} correctly.
     *
     * @param original
     *      Can be null.
     */
    public static HeaderList copy(HeaderList original) {
        if(original==null)
            return null;
        else
            return new HeaderList(original);
    }
}
