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

import com.sun.xml.ws.handler.MessageContextImpl;
import com.sun.xml.ws.sandbox.Decoder;
import com.sun.xml.ws.sandbox.Encoder;
import com.sun.xml.ws.sandbox.message.Message;
import com.sun.xml.ws.sandbox.message.MessageProperties;
import com.sun.xml.ws.sandbox.pipe.Pipe;
import com.sun.xml.ws.server.RuntimeEndpointInfo;
import com.sun.xml.ws.server.Tie;
import com.sun.xml.ws.spi.runtime.WSConnection;
import com.sun.xml.ws.spi.runtime.WebServiceContext;
import com.sun.xml.ws.transport.local.LocalMessage;
import com.sun.xml.ws.transport.local.server.LocalConnectionImpl;

import javax.xml.ws.WebServiceException;
import java.util.List;
import java.util.Map;

/**
 *
 * @author jitu
 */
public class LocalTransportPipe implements Pipe {
    
    private RuntimeEndpointInfo endpointInfo;
    private static final Tie tie = new Tie();
    LocalMessage lm = new LocalMessage();

    private final Encoder encoder;
    private final Decoder decoder;

    public LocalTransportPipe(RuntimeEndpointInfo endpointInfo, Encoder encoder, Decoder decoder) {
        this.endpointInfo = endpointInfo;
        this.encoder = encoder;
        this.decoder = decoder;
    }

    /**
     * Copy constructor for {@link #copy()}.
     */
    private LocalTransportPipe(LocalTransportPipe that) {
        this(that.endpointInfo,that.encoder.copy(),that.decoder.copy());
    }

    public void postConstruct() {
    }

    public Message process(Message msg) {
        
        try {
            // Set up WSConnection with tranport headers, request content

            LocalMessage lm = new LocalMessage();
            WSConnection con = new LocalConnectionImpl(lm);
            // get transport headers from message
            MessageProperties props = msg.getProperties();
            Map<String, List<String>> reqHeaders = props.httpRequestHeaders;
            con.setHeaders(reqHeaders);

            encoder.encode(msg, con.getOutput());

            // Set up RuntimeEndpointInfo with MessageContext

            // TODO: need to this somewhere once per RuntimeEndpointInfo
            WebServiceContext wsContext = endpointInfo.getWebServiceContext();
            // Set a MessageContext per invocation
            // TODO: Instead of MessageContextImpl use concrete class of MessageProperties
            wsContext.setMessageContext(new MessageContextImpl());

            tie.handle(con, endpointInfo);
            
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
        return null;
    }

    public void preDestroy() {
    }

    public Pipe copy() {
        return new LocalTransportPipe(this);
    }
}
