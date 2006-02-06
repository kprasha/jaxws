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
package com.sun.xml.ws.transport.local.client;

import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.Decoder;
import com.sun.xml.ws.api.pipe.Encoder;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.PipeCloner;
import com.sun.xml.ws.handler.MessageContextImpl;
import com.sun.xml.ws.server.RuntimeEndpointInfo;
import com.sun.xml.ws.server.Tie;
import com.sun.xml.ws.spi.runtime.WSConnection;
import com.sun.xml.ws.spi.runtime.WebServiceContext;
import com.sun.xml.ws.transport.local.LocalMessage;
import com.sun.xml.ws.transport.local.server.LocalConnectionImpl;

import javax.xml.ws.WebServiceException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Transport {@link Pipe} that routes a message to a service that runs within it.
 * <p/>
 * <p/>
 * This is useful to test the whole client-server in a single VM.
 *
 * @author jitu
 */
public class LocalTransportPipe implements Pipe {

    private RuntimeEndpointInfo endpointInfo;

    private final Encoder encoder;
    private final Decoder decoder;

    // per-pipe reusable resources.
    // we don't really have to reuse anything since this isn't designed for performance,
    // but nevertheless we do it as an experiement.
    private static final Tie tie = new Tie();
    private final LocalMessage lm = new LocalMessage();
    private final Map<String, List<String>> reqHeaders = new HashMap<String, List<String>>();

    public LocalTransportPipe(RuntimeEndpointInfo endpointInfo, Encoder encoder, Decoder decoder) {
        this.endpointInfo = endpointInfo;
        this.encoder = encoder;
        this.decoder = decoder;
    }

    /**
     * Copy constructor for {@link Pipe#copy(PipeCloner)}.
     */
    private LocalTransportPipe(LocalTransportPipe that, PipeCloner cloner) {
        this(that.endpointInfo, that.encoder.copy(), that.decoder.copy());
        cloner.add(that,this);
    }

    public Packet process(Packet packet) {

        try {
            // Set up WSConnection with tranport headers, request content

            WSConnection con = new LocalConnectionImpl(lm);
            // get transport headers from message
            reqHeaders.clear();
            if (packet.httpRequestHeaders != null)
                reqHeaders.putAll(packet.httpRequestHeaders);
            con.setHeaders(reqHeaders);

            String contentType = encoder.encode(packet, con.getOutput());

            reqHeaders.put("Content-Type", Arrays.asList(contentType));

            // Set up RuntimeEndpointInfo with MessageContext

            // TODO: need to this somewhere once per RuntimeEndpointInfo
            WebServiceContext wsContext = endpointInfo.getWebServiceContext();
            // Set a MessageContext per invocation
            // TODO: Instead of MessageContextImpl use concrete class of Packet
            wsContext.setMessageContext(new MessageContextImpl());

            tie.handle(con, endpointInfo);

            Map<String, List<String>> respHeaders = con.getHeaders();
            String ct = getContentType(respHeaders);

            if (packet.isOneWay == Boolean.TRUE
                || con.getStatus() == WSConnection.ONEWAY) {
                packet = new Packet(null);    // one way. no response given.
                packet.isOneWay = true;
                return packet;
            }

            return decoder.decode(con.getInput(), ct);
        } catch (WebServiceException wex) {
            throw wex;
        } catch (Exception ex) {
            throw new WebServiceException(ex);
        }
    }

    private String getContentType(Map<String, List<String>> headers) {
        return null;
    }

    public void preDestroy() {
    }

    public Pipe copy(PipeCloner cloner) {
        return new LocalTransportPipe(this,cloner);
    }
}
