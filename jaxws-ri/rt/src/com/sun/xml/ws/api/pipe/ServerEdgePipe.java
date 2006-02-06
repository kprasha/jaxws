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
 * Abstraction of the second pipe (next to the first or edge, network facing, 
 * pipe) in the processing line of a server.
 *
 * <p>
 * The edge {@link Pipe} in the processing line may check if the
 * second {@link Pipe} implements {@link ServerEdgePipe} and choose
 * (based on a hint on what the second {@link Pipe} requires) to send and
 * receive messages using lower level stream-based message representations
 * compared to that of {@link Message}.
 * 
 * <p>
 * The second {@link Pipe} can take on the role of an {@link Decoder} and/or 
 * {@link Encoder} and in doing so optimize the decoding of a stream-based 
 * message, represented as an {@link XMLStreamReader}, to a {@link Message}
 * and the encoding of a {@link Message} to a  stream-based message, 
 * represented as an {@link java.io.InputStream}. Such optimizations would be dependent
 * on the processing semantics of the {@link Pipe}.
 *
 * <p> 
 * A server side security pipe can use such an interface to optimize the 
 * production and verification of secure messages. The server security pipe is 
 * always configured to be the second pipe in the line, with the first pipe 
 * being the transport pipe.
 * When processing a secure request message the security pipe can
 * utilize, in certain cases, optimizations that operate on the direct 
 * infoset (decoded as an {@link XMLStreamReader}). The pipe can process this
 * infoset to produce a verified {@link Message} with all the security header
 * block information removed and encrypted information decrypted.
 * When producing a secure response message the security pipe can utilize, in 
 * certain cases, optimizations that produce an encoded message 
 * (encoded as an {@link java.io.InputStream}). This encoded message can then
 * be passed directly to the transport pipe, which does not perform any 
 * encoding.
 *
 */
public interface ServerEdgePipe {
    /**
     * Receives a {@link XMLStreamReader} and returns a response {@link Message}
     * to it.
     *
     * @throws WebServiceException
     *      see {@link Pipe#process(Packet)}.
     *
     * @throws RuntimeException
     *      see {@link Pipe#process(Packet)}.
     *
     * @param msg
     *      always a non-null unconsumed {@link XMLStreamReaderMessage} that
     *      represents a request.
     *
     * @return
     *      If this method returns a non-null value, it must be
     *      a valid unconsumed {@link Packet}. This message represents
     *      a response to the request message passed as a parameter.
     *      <p>
     *      This method is also allowed to return null, which indicates
     *      that there's no response. This is used for things like
     *      one-way message and/or one-way transports.
     */
    Packet processMessage(XMLStreamReaderMessage msg);

    /**
     * Receives a {@link javax.xml.stream.XMLStreamReader} and returns a response
     * {@link java.io.InputStream} to it.
     *
     * @throws WebServiceException
     *      see {@link Pipe#process(Packet)}.
     *
     * @throws RuntimeException
     *      see {@link Pipe#process(Packet)}.
     *
     * @param msg
     *      always a non-null unconsumed {@link XMLStreamReaderMessage} that
     *      represents a request.
     *
     * @return
     *      If this method returns a non-null value, it must be
     *      a valid unconsumed {@link InputStreamMessage}. 
     *      This message represents a response (as an {@link InputStream})
     *      to the request message passed as a parameter.
     *      <p>
     *      This method is also allowed to return null, which indicates
     *      that there's no response. This is used for things like
     *      one-way message and/or one-way transports.
     */
    InputStreamMessage processInputStream(XMLStreamReaderMessage msg);

    /**
     * Receives a {@link Message} and returns a response
     * {@link java.io.InputStream} to it.
     *
     * @throws WebServiceException
     *      see {@link Pipe#process(Packet)}.
     *
     * @throws RuntimeException
     *      see {@link Pipe#process(Packet)}.
     *
     * @param packet
     *      always a non-null valid unconsumed {@link Packet} that
     *      represents a request. See the <tt>packet</tt> parameter
     *      of {@link Pipe#process(Packet)}.
     *
     * @return
     *      If this method returns a non-null value, it must be
     *      a valid unconsumed {@link InputStreamMessage}. 
     *      This message represents a response (as an {@link InputStream})
     *      to the request message passed as a parameter.
     *      <p>
     *      This method is also allowed to return null, which indicates
     *      that there's no response. This is used for things like
     *      one-way message and/or one-way transports.
     */
    InputStreamMessage processInputStream(Packet packet);

    /**
     * Request response type enumerations.
     *
     */
    enum RequestResponseTypes {
        XML_STREAM_READER_REQUEST_MESSAGE_RESPONSE,
        XML_STREAM_READER_REQUEST_INPUT_STREAM_RESPONSE,
        MESSAGE_REQUEST_INPUT_STREAM_RESPONSE;
    }

    /**
     * Gets the request response type.
     *
     * This is a hint that informs the calling {@link Pipe} what is most
     * appropriate method to invoke on the invokee{@link Pipe}. The invoker
     * may ignore such a hint.
     * 
     * @return
     *     The request response hint.
     *     <p>
     *     If the hint is XML_STREAM_READER_REQUEST_MESSAGE_RESPONSE then the
     *     {@link ServerEdgePipe#processMessage(XMLStreamReaderMessage)} is the most
     *     appropriate method for the invoking {@link Pipe} to invoke.
     *     <p>
     *     If the hint is XML_STREAM_READER_REQUEST_INPUT_STREAM_RESPONSE then the
     *     {@link ServerEdgePipe#processInputStream(XMLStreamReaderMessage)} is the most
     *     appropriate method for the invoking {@link Pipe} to invoke.
     *     <p>
     *     If the hint is MESSAGE_REQUEST_INPUT_STREAM_RESPONSE then the
     *     {@link ServerEdgePipe#processInputStream(Message)} is the most
     *     appropriate method for the invoking {@link Pipe} to invoke.
     */
    RequestResponseTypes getRequestResponseType();
}
