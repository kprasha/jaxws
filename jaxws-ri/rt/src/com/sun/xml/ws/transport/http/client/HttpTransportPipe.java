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

import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.MessageProperties;
import com.sun.xml.ws.api.pipe.Decoder;
import com.sun.xml.ws.api.pipe.Encoder;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.PipeCloner;
import com.sun.xml.ws.spi.runtime.WSConnection;
import com.sun.xml.ws.util.ByteArrayBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import javax.xml.ws.WebServiceException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 *
 * @author jitu
 */
public class HttpTransportPipe implements Pipe {

    private final Encoder encoder;
    private final Decoder decoder;

    public HttpTransportPipe(Encoder encoder, Decoder decoder) {
        this.encoder = encoder;
        this.decoder = decoder;
    }

    /**
     * Copy constructor for {@link Pipe#copy(PipeCloner)}.
     */
    private HttpTransportPipe(HttpTransportPipe that) {
        this( that.encoder.copy(), that.decoder.copy() );
    }

    public Message process(Message msg) {
        try {
            // Set up WSConnection with tranport headers, request content
            // TODO: remove WSConnection based HttpClienTransport
            WSConnection con = new HttpClientTransport(null, msg.getProperties());

            // get transport headers from message
            MessageProperties props = msg.getProperties();
            Map<String, List<String>> reqHeaders = props.httpRequestHeaders;
            //assign empty map if its null
            if(reqHeaders == null){
                reqHeaders = new HashMap<String, List<String>>();
            }
            String ct = encoder.getStaticContentType();
            if (ct == null) {
                ByteArrayBuffer buf = new ByteArrayBuffer();
                ct = encoder.encode(msg, buf);
                // data size is available, set it as Content-Length
                reqHeaders.put("Content-Length", Arrays.asList(""+buf.size()));
                reqHeaders.put("Content-Type", Arrays.asList(ct));
                con.setHeaders(reqHeaders);
                buf.writeTo(con.getOutput());
            } else {
                // Set static Content-Type
                if (reqHeaders == null) {
                    reqHeaders = new HashMap<String, List<String>>();
                }
                reqHeaders.put("Content-Type", Arrays.asList(ct));
                con.setHeaders(reqHeaders);
                encoder.encode(msg, con.getOutput());
            }
            con.closeOutput();

            Map<String, List<String>> respHeaders = con.getHeaders();
            ct = getContentType(respHeaders);
            if(msg.getProperties().isOneWay==Boolean.TRUE
            || con.getStatus()==WSConnection.ONEWAY)
                return null;    // one way. no response given.

            return decoder.decode(con.getInput(), ct);
        } catch(WebServiceException wex) {
            throw wex;
        } catch(Exception ex) {
            throw new WebServiceException(ex);
        }
    }

    private String getContentType(Map<String, List<String>> headers) {
        return "text/xml";
    }

    public void preDestroy() {
    }

    public Pipe copy(PipeCloner cloner) {
        return new HttpTransportPipe(this);
    }
}
