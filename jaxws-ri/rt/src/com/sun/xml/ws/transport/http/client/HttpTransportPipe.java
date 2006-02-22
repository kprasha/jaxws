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
package com.sun.xml.ws.transport.http.client;

import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.Decoder;
import com.sun.xml.ws.api.pipe.Encoder;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.PipeCloner;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.spi.runtime.WSConnection;
import com.sun.xml.ws.util.ByteArrayBuffer;
import java.util.Arrays;
import java.util.HashMap;

import javax.xml.ws.WebServiceException;
import java.util.List;
import java.util.Map;

/**
 *
 * @author jitu
 */
public class HttpTransportPipe implements Pipe {

    private final Encoder encoder;
    private final Decoder decoder;

    public HttpTransportPipe(WSBinding binding) {
        this(binding.createEncoder(),binding.createDecoder());
    }

    private HttpTransportPipe(Encoder encoder, Decoder decoder) {
        this.encoder = encoder;
        this.decoder = decoder;
    }

    /**
     * Copy constructor for {@link Pipe#copy(PipeCloner)}.
     */
    private HttpTransportPipe(HttpTransportPipe that, PipeCloner cloner) {
        this( that.encoder.copy(), that.decoder.copy() );
        cloner.add(that,this);
    }

    public Packet process(Packet request) {
        try {
            // get transport headers from message
            Map<String, List<String>> reqHeaders = request.httpRequestHeaders;
            //assign empty map if its null
            if(reqHeaders == null){
                reqHeaders = new HashMap<String, List<String>>();
            }

            HttpClientTransport con = new HttpClientTransport(request,reqHeaders);

            String ct = encoder.getStaticContentType();
            if (ct == null) {
                ByteArrayBuffer buf = new ByteArrayBuffer();
                ct = encoder.encode(request, buf);
                // data size is available, set it as Content-Length
                reqHeaders.put("Content-Length", Arrays.asList(""+buf.size()));
                reqHeaders.put("Content-Type", Arrays.asList(ct));
                buf.writeTo(con.getOutput());
            } else {
                // Set static Content-Type
                reqHeaders.put("Content-Type", Arrays.asList(ct));
                encoder.encode(request, con.getOutput());
            }
            con.closeOutput();

            Map<String, List<String>> respHeaders = con.getHeaders();

            if (request.isOneWay==Boolean.TRUE
                || con.statusCode==WSConnection.ONEWAY) {
                return new Packet(null);    // one way. no response given.
            }
            ct = getContentType(respHeaders);
            Packet reply = request.createResponse(null);
            decoder.decode(con.getInput(), ct, reply);
            return reply;
        } catch(WebServiceException wex) {
            throw wex;
        } catch(Exception ex) {
            throw new WebServiceException(ex);
        }
    }

    private String getContentType(Map<String, List<String>> headers) {
        for(String key : headers.keySet()){
            if(key!= null && key.equalsIgnoreCase("Content-Type"))
                return headers.get(key).get(0);
        }
        //the response is invalid
        throw new WebServiceException("No Content-Type in the header!");
    }

    public void preDestroy() {
    }

    public Pipe copy(PipeCloner cloner) {
        return new HttpTransportPipe(this,cloner);
    }
}
