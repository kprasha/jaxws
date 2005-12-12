package com.sun.xml.ws.sandbox;

import com.sun.xml.ws.sandbox.message.Message;

import java.io.InputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

/**
 * The reverse operation of {@link Encoder}.
 *
 *
 * TODO: do we need a look up table from content type to {@link Decoder}?
 *
 * TODO: do we need to be able to get a corresponding {@link Encoder} from {@link Decoder}
 *       and vice versa?
 *
 * @author Kohsuke Kawaguchi
 */
public interface Decoder {
    /**
     * Reads bytes from {@link InputStream} and constructs a {@link Message}.
     *
     * <p>
     * The design encourages lazy decoding of a {@link Message}, where
     * a {@link Message} is returned even before the whole message is parsed,
     * and additional parsing is done as the {@link Message} body is read along.
     * A {@link Decoder} is most likely have its own implementation of {@link Message}
     * for this purpose.
     *
     * @param in
     *      the data to be read into a {@link Message}. The transport would have
     *      read any transport-specific header before it passes an {@link InputStream},
     *      and {@link InputStream} is expected to be read until EOS. Never null.
     * @param contentType
     *      The MIME content type (like "application/xml") of this byte stream.
     *      TODO: is this really necessary?
     *
     * @return never null.
     *
     * @throws IOException
     *      if {@link InputStream} throws an exception.
     */
    Message decode( InputStream in, String contentType ) throws IOException;

    /**
     *
     * @see #decode(InputStream, String)
     */
    Message decode( ReadableByteChannel in, String contentType );
}
