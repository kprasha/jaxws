package com.sun.xml.ws.sandbox;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;
import java.io.InputStream;

/**
 * {@link XMLStreamReader} extended for reading binary data.
 *
 * <p>
 * For more intuitive design, this interface would have extended {@link XMLStreamReader},
 * but that would require delegation, which introduces unnecessary overhead.
 *
 * @see XMLStreamWriterEx
 * @author Kohsuke Kawaguchi
 */
public interface XMLStreamReaderEx {
    /**
     * Gets the base {@link XMLStreamReader}.
     *
     * @return
     *      multiple invocation of this method must return
     *      the same object.
     */
    XMLStreamReader getBase();

    /**
     * Works like {@link XMLStreamReader#getText()}
     * but returns text as {@link DataSource}.
     *
     * <p>
     * This method can be invoked whenever {@link XMLStreamReader#getText()}
     * can be invoked. Invoking this method means the caller is assuming
     * that the text is (conceptually) base64-encoded binary data.
     *
     * <p>
     * This abstraction is necessary to treat XOP as infoset encoding.
     * That is, you can either access the XOP-attached binary through
     * {@link XMLStreamReader#getText()} (in which case you'll see the
     * base64 encoded string), or you can access it as a binary data
     * directly by using this method.
     *
     * <p>
     * Note that even if you are reading from non XOP-aware {@link XMLStreamReader},
     * this method must be still supported; if the reader is pointing
     * to a text, this method is responsible for decoding base64 and
     * producing a {@link DataHandler} with "application/octet-stream"
     * as the content type.
     *
     * @return
     *      always non-null valid object.
     *      Invocations of this method may return the same object as long
     *      as the {@link XMLStreamReader#next()} method is not used,
     *      but otherwise {@link DataSource} object returned from this method
     *      is considered to be owned by the client, and therefore it shouldn't
     *      be reused by the implementation of this method.
     *
     *      <p>
     *      The returned {@link DataSource} is read-only, and the caller
     *      must not invoke {@link DataSource#getOutputStream()}.
     *
     * @throws IllegalStateException
     *      if the parser is not pointing at characters infoset item.
     * @throws XMLStreamException
     *      if the parser points to text but text is not base64-encoded text,
     *      or if some other parsing error occurs (such as if the &lt;xop:Include>
     *      points to a non-existing attachment.)
     *
     *      <p>
     *      It is also OK for this method to return successfully, only to fail
     *      during an {@link InputStream} is read from {@link DataSource}.
     */
    DataSource getTextAsDataHandler() throws XMLStreamException;

    /**
     * Works like {@link XMLStreamReader#getText()}
     * but returns text as {@link byte[]}.
     *
     * <p>
     * The contract of this method is mostly the same as
     * {@link #getTextAsDataHandler()}, except that this
     * method returns the binary datas as an exact-size byte[].
     *
     * <p>
     * This method is also not capable of reporting the content type
     * of this binary data, even if it is available to the parser.
     *
     * @see #getTextAsDataHandler()
     */
    byte[] getTextAsByteArray() throws XMLStreamException;

}
