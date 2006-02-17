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
package com.sun.xml.ws.transport.local;

import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.server.WSEndpoint;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.Decoder;
import com.sun.xml.ws.api.pipe.Encoder;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.PipeCloner;
import com.sun.xml.ws.api.server.Adapter;
import com.sun.xml.ws.spi.runtime.WSConnection;
import com.sun.xml.ws.transport.http.HttpAdapter;

import javax.xml.ws.WebServiceException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.IOException;

/**
 * Transport {@link Pipe} that routes a message to a service that runs within it.
 *
 * <p>
 * This is useful to test the whole client-server in a single VM.
 *
 * @author jitu
 */
final class LocalTransportPipe implements Pipe {

    /**
     * Represents the service running inside the local transport.
     *
     * We use {@link HttpAdapter}, so that the local transport
     * excercise as much server code as possible. If this were
     * to be done "correctly" we should write our own {@link Adapter}
     * for the local transport.
     */
    private final HttpAdapter adapter;

    private final Encoder encoder;
    private final Decoder decoder;

    // per-pipe reusable resources.
    // we don't really have to reuse anything since this isn't designed for performance,
    // but nevertheless we do it as an experiement.
    private final Map<String, List<String>> reqHeaders = new HashMap<String, List<String>>();

    public LocalTransportPipe(WSEndpoint endpoint, WSBinding binding) {
        this(new HttpAdapter(endpoint),binding.createEncoder(),binding.createDecoder());
    }

    private LocalTransportPipe(HttpAdapter adapter, Encoder encoder, Decoder decoder) {
        this.adapter = adapter;
        this.encoder = encoder;
        this.decoder = decoder;
        assert encoder!=null && decoder!=null && adapter!=null;
    }

    /**
     * Copy constructor for {@link Pipe#copy(PipeCloner)}.
     */
    private LocalTransportPipe(LocalTransportPipe that, PipeCloner cloner) {
        this(that.adapter, that.encoder.copy(), that.decoder.copy());
        cloner.add(that,this);
    }

    public Packet process(Packet request) {

        try {
            // Set up WSConnection with tranport headers, request content

            LocalConnectionImpl con = new LocalConnectionImpl();
            // get transport headers from message
            reqHeaders.clear();
            if (request.httpRequestHeaders != null)
                reqHeaders.putAll(request.httpRequestHeaders);
            con.setResponseHeaders(reqHeaders);

            String contentType = encoder.encode(request, con.getOutput());

            reqHeaders.put("Content-Type", Arrays.asList(contentType));
            con.setRequestHeaders(reqHeaders);

            adapter.handle(con);

            String ct = getContentType(con);

            if (request.isOneWay == Boolean.TRUE
                || con.getStatus() == WSConnection.ONEWAY) {
                request = new Packet(null);    // one way. no response given.
                request.isOneWay = true;
                return request;
            }

            Packet reply = request.createResponse(null);
            decoder.decode(con.getInput(), ct, reply);
            return reply;
        } catch (WebServiceException wex) {
            throw wex;
        } catch (IOException ex) {
            throw new WebServiceException(ex);
        }
    }

    private String getContentType(LocalConnectionImpl con) {
        Map<String, List<String>> rsph = con.getResponseHeaders();
        if(rsph!=null) {
            List<String> c = rsph.get("Content-Type");
            if(c!=null && !c.isEmpty())
                return c.get(0);
        }
        return null;
    }

    public void preDestroy() {
    }

    public Pipe copy(PipeCloner cloner) {
        return new LocalTransportPipe(this,cloner);
    }
}
