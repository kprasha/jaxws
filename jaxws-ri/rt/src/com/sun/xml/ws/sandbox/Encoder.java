package com.sun.xml.ws.sandbox;

import com.sun.xml.ws.sandbox.message.Message;

import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/**
 * Encodes a {@link Message} (its XML infoset and attachments) to a sequence of bytes.
 *
 * <p>
 * This interface provides pluggability for different ways of encoding XML infoset,
 * such as plain XML (plus MIME attachments), XOP, and FastInfoset.
 *
 * <p>
 * Transport usually needs a MIME content type of the encoding, so the {@link Encoder}
 * interface is designed to return this information. However, for some encoding
 * (such as XOP), the encoding may actually change based on the actual content of
 * {@link Message}, therefore the encoder returns the content type as a result of encoding.
 *
 * <p>
 * {@link Encoder} does not produce transport-specific information, such as HTTP headers.
 *
 * <p>
 * {@link Encoder} is a non-reentrant object, meaning no two threads
 * can concurrently invoke the decode method. This allows the implementation
 * to easily reuse parser objects (as instance variables), which are costly otherwise.
 *
 * @author Kohsuke Kawaguchi
 */
public interface Encoder {

    /**
     * If the MIME content-type of the encoding is known statically
     * then this method returns it.
     *
     * <p>
     * Transports often need to write the content type before it writes
     * the message body, and since the encode method returns the content type
     * after the body is written, it requires a buffering.
     *
     * For those {@link Encoder}s that always use a constant content type,
     * This method allows a transport to streamline the write operation.
     *
     * @return
     *      null if the content-type may change from a {@link Message} to {@link Message}.
     *      Otherwise return the static content type, like "application/xml".
     */
    String getStaticContentType();

    /**
     * Encodes an XML infoset portion of the {@link Message}
     * (from &lt;soap:Envelope> to &lt;/soap:Envelope>).
     *
     * <p>
     * Internally, this method is most likely invoke {@link Message#writeTo(XMLStreamWriter)}
     * to turn the message into infoset.
     *
     * @param out
     *      Must not be null. The caller is responsible for closing the stream,
     *      not the callee.
     *
     * @return
     *      The MIME content type of the encoded message (such as "application/xml").
     *      This information is often ncessary by transport.
     *
     * @throws IOException
     *      if a {@link OutputStream} throws {@link IOException}.
     */
    String encode( Message message, OutputStream out ) throws IOException;

    /**
     * The version of {@link #encode(Message, OutputStream)}
     * that writes to NIO {@link ByteBuffer}.
     *
     * <p>
     * TODO: for the convenience of implementation, write
     * an adapter that wraps {@link WritableByteChannel} to {@link OutputStream}.
     */
    String encode( Message message, WritableByteChannel buffer );

    /**
     * Creates a copy of this {@link Encoder}.
     *
     * <p>
     * Since {@link Encoder} instance is not re-entrant, the caller
     * who needs to encode two {@link Message}s simultaneously will
     * want to have two {@link Encoder} instances. That's what this
     * method produces.
     *
     * <h3>Implentation Note</h3>
     * <p>
     * Note that this method might be invoked by one thread while
     * another thread is executing one of the {@link #encode} methods.
     * <!-- or otherwise you'd always have to maintain one idle copy -->
     * <!-- just so that you can make copies from -->
     * This should be OK because you'll be only copying things that
     * are thread-safe, and creating new ones for thread-unsafe resources,
     * but please let us know if this contract is difficult.
     *
     * @return
     *      always non-null valid {@link Encoder} that performs
     *      the encoding work in the same way --- that is, if you
     *      copy an FI encoder, you'll get another FI encoder.
     *
     *      <p>
     *      Once copied, two {@link Encoder}s may be invoked from
     *      two threads concurrently; therefore, they must not share
     *      any state that requires isolation (such as temporary buffer.)
     *
     *      <p>
     *      If the {@link Encoder} implementation is already
     *      re-entrant and multi-thread safe to begin with,
     *      then this method may simply return <tt>this</tt>.
     */
    Encoder copy();
}
