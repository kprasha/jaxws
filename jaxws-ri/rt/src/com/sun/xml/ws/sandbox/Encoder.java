package com.sun.xml.ws.sandbox;

import com.sun.xml.ws.sandbox.message.Message;

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
     * Internally, this method is most likely invoke {@link Message#writeTo(XMLStreamWriterEx)}
     * to turn the message into infoset.
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
}
