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

import com.sun.xml.ws.sandbox.Decoder;
import com.sun.xml.ws.sandbox.Encoder;
import com.sun.xml.ws.sandbox.message.Message;
import com.sun.xml.ws.sandbox.message.MessageProperties;
import com.sun.xml.ws.sandbox.pipe.Pipe;
import com.sun.xml.ws.spi.runtime.WSConnection;
import com.sun.xml.ws.transport.WSConnectionImpl;

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

    private MessageProperties context;

    // TODO: what's this 'context'? please document.
    public HttpTransportPipe(Encoder encoder, Decoder decoder,  MessageProperties context) {
        this.encoder = encoder;
        this.decoder = decoder;
        this.context = context;  //TODO:can get messageprops from message do not need this
    }

    /**
     * Copy constructor for {@link #copy()}.
     */
    private HttpTransportPipe(HttpTransportPipe that) {
        this( that.encoder.copy(), that.decoder.copy(), that.context );
    }

    public void postConstruct() {
    }

    public Message process(Message msg) {

        try {
            // Set up WSConnection with tranport headers, request content
            // TODO: remove WSConnection based HttpClienTransport
            WSConnection con = new HttpClientTransport(null, context);

            // get transport headers from message
            MessageProperties props = msg.getProperties();
            Map<String, List<String>> reqHeaders = props.httpRequestHeaders;
            con.setHeaders(reqHeaders);
            encoder.encode(msg, con.getOutput());
            con.closeOutput();

            Map<String, List<String>> respHeaders = con.getHeaders();
            String ct = getContentType(respHeaders);
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

    public Pipe copy() {
        return new HttpTransportPipe(this);
    }
}
