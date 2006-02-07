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
package com.sun.xml.ws.api.pipe;

import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.WSBinding;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;

/**
 * The reverse operation of {@link Encoder}.
 *
 * <p>
 * {@link Decoder} is a non-reentrant object, meaning no two threads
 * can concurrently invoke the decode method. This allows the implementation
 * to easily reuse parser objects (as instance variables), which are costly otherwise.
 *
 *
 * <p>
 * {@link WSBinding} determines the {@link Decoder}. See {@link WSBinding#createDecoder()}.
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
     *
     *      <p>
     *      Some transports, such as SMTP, may 'encode' data into another format
     *      (such as uuencode, base64, etc.) It is the caller's responsibility to
     *      'decode' these transport-level encoding before it passes data into
     *      {@link Decoder}.
     *
     * @param contentType
     *      The MIME content type (like "application/xml") of this byte stream.
     *      Thie text includes all the sub-headers of the content-type header. Therefore,
     *      in more complex case, this could be something like
     *      <tt>multipart/related; boundary="--=_outer_boundary"; type="multipart/alternative"</tt>.
     *      This parameter must not be null.
     *
     * @return never null.
     *
     * @throws IOException
     *      if {@link InputStream} throws an exception.
     */
    Packet decode( InputStream in, String contentType ) throws IOException;

    /**
     *
     * @see #decode(InputStream, String)
     */
    Packet decode( ReadableByteChannel in, String contentType );

    /*
     * The following methods need to be documented and implemented.
     *
     * Such methods will be used by a server side
     * transport pipe that can support the invocation of methods on a
     * ServerEdgePipe.
     *
    XMLStreamReaderMessage decode( InputStream in, String contentType ) throws IOException;
    XMLStreamReaderMessage decode( ReadableByteChannel in, String contentType );
    */
    
    /**
     * Creates a copy of this {@link Decoder}.
     *
     * <p>
     * See {@link Encoder#copy()} for the detailed contract.
     */
    Decoder copy();
}
