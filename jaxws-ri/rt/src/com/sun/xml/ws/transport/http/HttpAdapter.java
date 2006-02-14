package com.sun.xml.ws.transport.http;

import com.sun.xml.ws.api.server.WSEndpoint;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.server.Adapter;
import com.sun.xml.ws.api.server.DocumentAddressResolver;
import com.sun.xml.ws.api.server.SDDocument;
import com.sun.xml.ws.api.server.ServiceDefinition;
import com.sun.xml.ws.api.server.TransportBackChannel;
import com.sun.xml.ws.spi.runtime.WSConnection;
import com.sun.xml.ws.transport.Headers;
import com.sun.xml.ws.util.localization.LocalizableMessageFactory;
import com.sun.xml.ws.util.localization.Localizer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * {@link Adapter} that receives messages in HTTP.
 *
 * <p>
 * This object also assigns unique query string (such as "xsd=1") to
 * each {@link SDDocument} so that they can be served by HTTP GET requests.
 *
 * @author Kohsuke Kawaguchi
 */
public class HttpAdapter extends Adapter<HttpAdapter.HttpToolkit> {

    /**
     * {@link SDDocument}s keyed by the query string like "?abc".
     * Used for serving documents via HTTP GET.
     *
     * Empty if the endpoint doesn't have {@link ServiceDefinition}.
     * Read-only.
     */
    public final Map<String,SDDocument> wsdls;

    /**
     * Reverse map of {@link #wsdls}. Read-only.
     */
    public final Map<SDDocument,String> revWsdls;


    public HttpAdapter(WSEndpoint endpoint) {
        super(endpoint);

        // fill in WSDL map
        ServiceDefinition sdef = this.endpoint.getServiceDefinition();
        if(sdef==null) {
            wsdls = Collections.emptyMap();
            revWsdls = Collections.emptyMap();
        } else {
            wsdls = new HashMap<String, SDDocument>();
            int wsdlnum = 1;
            int xsdnum = 1;
            for (SDDocument sdd : sdef) {
                if (sdd == sdef.getPrimary()) {
                    wsdls.put("wsdl", sdd);
                } else {
                    if(sdd.isWSDL()) {
                        wsdls.put("wsdl="+(wsdlnum++),sdd);
                    }
                    if(sdd.isSchema()) {
                        wsdls.put("xsd="+(xsdnum++),sdd);
                    }
                }
            }

            revWsdls = new HashMap<SDDocument,String>();
            for (Entry<String,SDDocument> e : wsdls.entrySet()) {
                revWsdls.put(e.getValue(),e.getKey());
            }
        }
    }


    final class HttpToolkit extends Adapter.Toolkit implements TransportBackChannel {
        private WSConnection con;

        public void handle(WSConnection con) throws IOException {
            this.con = con;
            String ct = con.getRequestHeader("Content-Type");
            InputStream in = con.getInput();
            Packet packet = decoder.decode(in, ct);
            con.wrapUpRequestPacket(packet);
            packet = head.process(packet,con.getWebServiceContextDelegate(),this);

            ct = encoder.getStaticContentType();
            if (ct == null) {
                throw new UnsupportedOperationException();
            } else {
                //headers = msg.getProperties().HTTP_RESPONSE_HEADERS;
                Headers headers = new Headers();
                headers.put("Content-Type", Collections.singletonList(ct));
                con.setResponseHeaders(headers);
                if (packet.getMessage()==null) {
                    con.setStatus(WSConnection.ONEWAY);
                } else {
                    encoder.encode(packet, con.getOutput());
                }
            }
        }

        public void close() {
            // close the response channel now
            con.setStatus(WSConnection.ONEWAY);
            con.closeOutput();
        }
    }

    protected HttpToolkit createToolkit() {
        return new HttpToolkit();
    }

    /**
     * Receives the incoming HTTP connection and dispatches
     * it to JAX-WS.
     *
     * <p>
     * This method is invoked by the lower-level HTTP stack,
     * and "connection" here is an HTTP connection.
     *
     * <p>
     * To populate a request {@link Packet} with more info,
     * use {@link WSConnection#wrapUpRequestPacket(Packet)}.
     */
    public void handle(WSConnection connection) throws IOException {
        HttpToolkit tk = pool.take();
        try {
            tk.handle(connection);
        } finally {
            pool.recycle(tk);
        }
    }

    /**
     * Returns true if the given query string is for metadata request.
     *
     * @param query
     *      String like "xsd=1" or "perhaps=some&amp;unrelated=query".
     *      Can be null.
     */
    public final boolean isMetadataQuery(String query) {
        // we intentionally return true even if documents don't exist,
        // so that they get 404.
        return query != null && (query.startsWith("wsdl") || query.startsWith("xsd="));
    }

    /**
     * Sends out the WSDL (and other referenced documents)
     * in response to the GET requests to URLs like "?wsdl" or "?xsd=2".
     *
     * @param con
     *      The connection to which the data will be sent. Must not be null.
     * @param baseAddress
     *      The requested base URL (such as "http://myhost:2045/foo/bar").
     *      Used to reference other resoures. Must not be null.
     * @param queryString
     *      The query string given by the client (which indicates
     *      what document to serve.) Can be null (but it causes an 404 not found.)
     */
    public void publishWSDL(WSConnection con, final String baseAddress, String queryString) throws IOException {

        SDDocument doc = wsdls.get(queryString);
        if (doc == null) {
            writeNotFoundErrorPage(con,"Invalid Request");
            return;
        }

        con.setStatus(SC_OK);
        setContentType(con, "application/xml");

        OutputStream os = con.getOutput();
        DocumentAddressResolver resolver = new DocumentAddressResolver() {
            public String getRelativeAddressFor(SDDocument current, SDDocument referenced) {
                // the map on endpoint should account for all SDDocument
                assert revWsdls.containsKey(referenced);
                return baseAddress+'?'+ revWsdls.get(referenced);
            }
        };
        doc.writeTo(baseAddress,resolver,os);
    }

    private void writeNotFoundErrorPage(WSConnection con, String message) throws IOException {
        con.setStatus(SC_NOT_FOUND);
        setContentType(con, "text/html; charset=UTF-8");

        PrintWriter out = new PrintWriter(new OutputStreamWriter(con.getOutput(),"UTF-8"));
        out.println("<html>");
        out.println("<head><title>");
        out.println(
            localizer.localize(
                messageFactory.getMessage("servlet.html.title")));
        out.println("</title></head>");
        out.println("<body>");
        out.println(
            localizer.localize(
                messageFactory.getMessage("servlet.html.notFound", message)));
        out.println("</body>");
        out.println("</html>");
    }

    /**
     * Sets the Content-Type as the only header.
     */
    private void setContentType(WSConnection con, String contentType) {
        con.setResponseHeaders(Collections.singletonMap("Content-Type",Collections.singletonList(contentType)));
    }

    private static final Localizer localizer = new Localizer();
    private static final LocalizableMessageFactory messageFactory =
        new LocalizableMessageFactory("com.sun.xml.ws.resources.wsservlet");


    private static final int SC_OK = 200;
    private static final int SC_NOT_FOUND = 404;
}
