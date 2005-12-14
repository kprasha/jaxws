package com.sun.xml.ws.sandbox;

import com.sun.xml.ws.sandbox.impl.XMLStreamWriterExImpl;

import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLStreamException;
import javax.activation.DataHandler;
import java.io.OutputStream;

/**
 * {@link XMLStreamWriter} extended to support XOP.
 *
 * <p>
 * For more intuitive design, this interface would have extended {@link XMLStreamWriter},
 * but that would require delegation, which introduces unnecessary overhead.
 *
 * <h2>TODO</h2>
 * <ol>
 * <li>
 *   Add methods to write other primitive types, such as hex and integers
 *   (and arrays of).
 *   A textual implementation would write characters in accordance
 *   to the canonical lexical definitions specified in W3C XML Schema: datatypes.
 *   A MTOM implementation would write characters except for the case where octets
 *   that would otherwise be base64 encoded when using the textual implementation.
 *   A Fast Infoset implementation would encoded binary data the primitive types in
 *   binary form.
 * <li>
 *   Consider renaming writeBinary to writeBytesAsBase64 to be consistent with
 *   infoset abstraction.
 * <li>
 *   Add the ability to writeStart and writeEnd on attributes so that the same
 *   methods for writing primitive types (and characters, which will require new methods)
 *   can be used for writing attribute values as well as element content.
 * </ol>
 *
 * @see XMLStreamReaderEx
 * @see XMLStreamWriterExImpl
 * @author Kohsuke Kawaguchi
 */
public interface XMLStreamWriterEx {

    /**
     * Gets the base {@link XMLStreamWriter}.
     *
     * @return
     *      multiple invocation of this method must return
     *      the same object.
     */
    XMLStreamWriter getBase();

    /**
     * Write the binary data.
     *
     * <p>
     * Conceptually (infoset-wise), this produces the base64-encoded binary data on the
     * output. But this allows implementations like FastInfoset or XOP to do the smart
     * thing.
     *
     * <p>
     * The use of this method has some restriction to support XOP. Namely, this method
     * must be invoked as a sole content of an element.
     *
     * <p>
     * (data,start,len) triplet identifies the binary data to be written.
     *
     * @param contentType
     *      this mandatory parameter identifies the MIME type of the binary data.
     *      If the MIME type isn't known by the caller, "application/octet-stream" can
     *      be always used to indicate "I don't know." Never null.
     */
    void writeBinary(byte[] data, int start, int len, String contentType) throws XMLStreamException;

    /**
     * Writes the binary data.
     *
     * <p>
     * This method works like the {@link #writeBinary(byte[], int, int, String)} method,
     * except that it takes the binary data in the form of {@link DataHandler}, which
     * contains a MIME type ({@link DataHandler#getContentType()} as well as the payload
     * {@link DataHandler#getInputStream()}.
     *
     * @param data
     *      always non-null.
     */
    void writeBinary(DataHandler data) throws XMLStreamException;

    /**
     * Writes the binary data.
     *
     * <p>
     * This version of the writeBinary method allows the caller to produce
     * the binary data by writing it to {@link OutputStream}.
     *
     * <p>
     * It is the caller's responsibility to write and close
     * a stream before it invokes any other methods on {@link XMLStreamWriterEx}
     * or {@link XMLStreamWriter}.
     *
     * TODO: experimental. appreciate feedback
     * @param contentType
     *      See the content-type parameter of
     *      {@link #writeBinary(byte[], int, int, String)}. Must not be null.
     *
     * @return
     *      always return a non-null {@link OutputStream}.
     */
    OutputStream writeBinary(String contentType) throws XMLStreamException;
}
