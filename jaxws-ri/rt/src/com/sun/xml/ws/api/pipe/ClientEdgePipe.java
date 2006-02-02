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
import com.sun.xml.ws.api.message.stream.InputStreamMessage;
import com.sun.xml.ws.api.message.stream.XMLStreamReaderMessage;

import javax.xml.ws.WebServiceException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;

/**
 * Abstraction of the last pipe (the edge, networking facing, pipe) in the 
 * processing line of a client.
 *
 * <p>
 * The next to last {@link Pipe} in the processing line may check if
 * the edge {@link Pipe} implements {@link ClientEdgePipe} and choose
 * to send and receive messages using lower level stream-based message 
 * representations compared to that of {@link Message}.
 * 
 * <p>
 * The next to last {@link Pipe} can take on the role of
 * an {@link Encoder} and/or {@link Decoder} and in doing so optimize
 * the encoding of a {@link Message} to a stream-based message, 
 * represented as an {@link InputStream}, and the decoding of a
 * stream-based message, represented as an {@link XMLStreamReader}, to
 * a {@link Message}. Such optimizations would be dependent
 * on the processing semantics of the {@link Pipe}.
 *
 * <p> 
 * A client side security pipe can use such an interface to optimize the 
 * production and verification of secure messages. The client security pipe is 
 * always configured to be the next to last pipe in the line, with the last 
 * pipe being transport pipe. 
 * When producing a secure request message the security pipe can utilize, in 
 * certain cases, optimizations that produce an encoded message 
 * (encoded as an {@link InputStream}). This encoded message can then
 * be passed directly to the transport pipe, which does not perform any 
 * encoding.
 * When processing a secure response message the security pipe can
 * utilize, in certain cases, optimizations that operate on the direct 
 * infoset (decoded as an {@link XMLStreamReader}). The pipe can process this
 * infoset to produce a verified {@link Message} with all the security header
 * block information removed and encrypted information decrypted.
 *
 */
public interface ClientEdgePipe {
    /**
     * Sends a {@link Message} and returns a response {@link XMLStreamReader}
     * to it.
     *
     * @throws WebServiceException
     *      see {@link Pipe#process(Packet)}.
     *
     * @throws RuntimeException
     *      see {@link Pipe#process(Packet)}.
     *
     * @param msg
     *      always a non-null valid unconsumed {@link Message} that
     *      represents a request.
     *      The callee may consume a {@link Message} (and in fact
     *      most of the time it will), and therefore once a {@link Message}
     *      is given to a {@link Pipe}.
     *
     * @return
     *      If this method returns a non-null value, it must be
     *      a valid unconsumed {@link XMLStreamReaderMessage}. 
     *      This message represents a response (as an {@link XMLStreamReader})
     *      to the request message passed as a parameter.
     *      <p>
     *      This method is also allowed to return null, which indicates
     *      that there's no response. This is used for things like
     *      one-way message and/or one-way transports.
     */
    XMLStreamReaderMessage processStreamReader(Message msg);

    /**
     * Sends a {@link InputStream} and returns a response {@link XMLStreamReader}
     * to it.
     *
     * @throws WebServiceException
     *      see {@link Pipe#process(Packet)}.
     *
     * @throws RuntimeException
     *      see {@link Pipe#process(Packet)}.
     *
     * @param msg
     *      always a non-null unconsumed {@link InputStreamMessage} that
     *      represents a request.
     *
     * @return
     *      If this method returns a non-null value, it must be
     *      a valid unconsumed {@link XMLStreamReaderMessage}. 
     *      This message represents a response (as an {@link XMLStreamReader})
     *      to the request message passed as a parameter.
     *      <p>
     *      This method is also allowed to return null, which indicates
     *      that there's no response. This is used for things like
     *      one-way message and/or one-way transports.
     */
    XMLStreamReaderMessage processStreamReader(InputStreamMessage msg);

    /**
     * Sends a {@link InputStream} and returns a response {@link Message}
     * to it.
     *
     * @throws WebServiceException
     *      see {@link Pipe#process(Packet)}.
     *
     * @throws RuntimeException
     *      see {@link Pipe#process(Packet)}.
     *
     * @param msg
     *      always a non-null unconsumed {@link InputStreamMessage} that
     *      represents a request.
     *
     * @return
     *      If this method returns a non-null value, it must be
     *      a valid unconsumed {@link Message}. This message represents
     *      a response to the request message passed as a parameter.
     *      <p>
     *      This method is also allowed to return null, which indicates
     *      that there's no response. This is used for things like
     *      one-way message and/or one-way transports.
     */
    Message processMessage(InputStreamMessage msg);
}
