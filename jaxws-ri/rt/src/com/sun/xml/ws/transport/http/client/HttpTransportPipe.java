/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License).  You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the license at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * 
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.xml.ws.transport.http.client;

import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.Codec;
import com.sun.xml.ws.api.pipe.ContentType;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.PipeCloner;
import com.sun.xml.ws.transport.http.WSHTTPConnection;
import com.sun.xml.ws.util.ByteArrayBuffer;

import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.MessageContext;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * {@link Pipe} that sends a request to a remote HTTP server.
 *
 * @author jitu
 */
public class HttpTransportPipe implements Pipe {

    private final Codec codec;

    public HttpTransportPipe(WSBinding binding) {
        this(binding.createCodec());
    }

    private HttpTransportPipe(Codec codec) {
        this.codec = codec;
    }

    /**
     * Copy constructor for {@link Pipe#copy(PipeCloner)}.
     */
    private HttpTransportPipe(HttpTransportPipe that, PipeCloner cloner) {
        this( that.codec.copy() );
        cloner.add(that,this);
    }

    public Packet process(Packet request) {
        try {
            // get transport headers from message
            Map<String, List<String>> reqHeaders = (Map<String, List<String>>) request.invocationProperties.get(MessageContext.HTTP_REQUEST_HEADERS);
            //assign empty map if its null
            if(reqHeaders == null){
                reqHeaders = new HashMap<String, List<String>>();
            }

            HttpClientTransport con = new HttpClientTransport(request,reqHeaders);

            ContentType ct = codec.getStaticContentType(request);
            if (ct == null) {
                ByteArrayBuffer buf = new ByteArrayBuffer();
                ct = codec.encode(request, buf);
                // data size is available, set it as Content-Length
                reqHeaders.put("Content-Length", Collections.singletonList(Integer.toString(buf.size())));
                reqHeaders.put("Content-Type", Collections.singletonList(ct.getContentType()));
                writeSOAPAction(reqHeaders, ct.getSOAPActionHeader());
                if(dump)
                    dump(buf, "HTTP request", reqHeaders);
                buf.writeTo(con.getOutput());
            } else {
                // Set static Content-Type
                reqHeaders.put("Content-Type", Collections.singletonList(ct.getContentType()));
                writeSOAPAction(reqHeaders, ct.getSOAPActionHeader());
                if(dump) {
                    ByteArrayBuffer buf = new ByteArrayBuffer();
                    codec.encode(request, buf);
                    dump(buf, "HTTP request", reqHeaders);
                    buf.writeTo(con.getOutput());
                } else {
                    codec.encode(request, con.getOutput());
                }
            }

            con.closeOutput();

            Map<String, List<String>> respHeaders = con.getHeaders();

            if (con.statusCode== WSHTTPConnection.ONEWAY) {
                return request.createResponse(null);    // one way. no response given.
            }
            String contentType = getContentType(respHeaders);
            Packet reply = request.createResponse(null);
            reply.addSatellite(new HttpResponseProperties(con.getConnection()));
            InputStream response = con.getInput();
            if(dump) {
                ByteArrayBuffer buf = new ByteArrayBuffer();
                buf.write(response);
                dump(buf,"HTTP response "+con.statusCode, respHeaders);
                response = buf.newInputStream();
            }
            codec.decode(response, contentType, reply);

            return reply;
        } catch(WebServiceException wex) {
            throw wex;
        } catch(Exception ex) {
            throw new WebServiceException(ex);
        }
    }

    /**
     * write SOAPAction header if the soapAction parameter is non-null
     */
    private void writeSOAPAction(Map<String, List<String>> reqHeaders, String soapAction) {
        if(soapAction != null){
            reqHeaders.put("SOAPAction", Collections.singletonList(soapAction));
        }
    }

    private String getContentType(Map<String, List<String>> headers) {
        List<String> keys = headers.get("Content-type");
        if (keys == null) {
            //the response is invalid
            throw new WebServiceException("No Content-Type in the header!");
        }
        return keys.get(0);
    }

    public void preDestroy() {
    }

    public Pipe copy(PipeCloner cloner) {
        return new HttpTransportPipe(this,cloner);
    }

    private void dump(ByteArrayBuffer buf, String caption, Map<String, List<String>> headers) throws IOException {
        System.out.println("---["+caption +"]---");
        for (Entry<String,List<String>> header : headers.entrySet()) {
            if(header.getValue().isEmpty()) {
                // I don't think this is legal, but let's just dump it,
                // as the point of the dump is to uncover problems.
                System.out.println(header.getValue());
            } else {
                for (String value : header.getValue()) {
                    System.out.println(header.getValue()+": "+value);
                }
            }
        }

        buf.writeTo(System.out);
        System.out.println("--------------------");
    }

    /**
     * Dumps what goes across HTTP transport.
     */
    private static final boolean dump;

    static {
        boolean b;
        try {
            b = Boolean.getBoolean(HttpTransportPipe.class.getName()+".dump");
        } catch( Throwable t ) {
            b = false;
        }
        dump = b;
    }
}
