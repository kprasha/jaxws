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

package com.sun.xml.ws.transport.http.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsExchange;
import com.sun.xml.ws.spi.runtime.WSConnection;
import com.sun.xml.ws.transport.http.HttpAdapter;
import com.sun.xml.ws.util.localization.LocalizableMessageFactory;
import com.sun.xml.ws.util.localization.Localizer;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link HttpHandler} implementation that serves the actual request.
 *
 * @author WS Development Team
 * @author Kohsuke Kawaguhi
 */
final class WSHttpHandler implements HttpHandler {

    private static final String GET_METHOD = "GET";
    private static final String POST_METHOD = "POST";

    private static final Logger logger =
        Logger.getLogger(
            com.sun.xml.ws.util.Constants.LoggingDomain + ".server.http");
    private static final Localizer localizer = new Localizer();
    private static final LocalizableMessageFactory messageFactory =
        new LocalizableMessageFactory("com.sun.xml.ws.resources.httpserver");

    private final HttpAdapter adapter;

    public WSHttpHandler(HttpAdapter adapter) {
        assert adapter!=null;
        this.adapter = adapter;
    }

    public void handle(HttpExchange msg) {
        WSConnection con = new ServerConnectionImpl(msg);

        try {
            logger.fine("Received HTTP request:"+msg.getRequestURI());

            String method = msg.getRequestMethod();
            if (method.equals(GET_METHOD)) {
                String queryString = msg.getRequestURI().getQuery();
                logger.fine("Query String for request ="+queryString);
                if (adapter.isMetadataQuery(queryString)) {
                    adapter.publishWSDL(con,getRequestAddress(msg), msg.getRequestURI().getQuery());
                } else {
                    adapter.handle(con);
                }
            } else if (method.equals(POST_METHOD)) {
                adapter.handle(con);
            } else {
                logger.warning(
                    localizer.localize(
                        messageFactory.getMessage(
                            "unexpected.http.method", method)));
            }
        } catch(IOException e) {
            logger.log(Level.WARNING, e.getMessage(),e);
        } finally {
            con.close();
        }
    }

    /**
     * Computes the address that was requested.
     *
     * @return
     *      a string like "http://foo.bar:1234/abc/def"
     */
    private String getRequestAddress(HttpExchange msg) {
        StringBuilder strBuf = new StringBuilder();
        strBuf.append((msg instanceof HttpsExchange) ? "https" : "http");
        strBuf.append("://");

        List<String> hostHeader = msg.getResponseHeaders().get("Host");
        if (hostHeader != null) {
            strBuf.append(hostHeader.get(0));   // Uses Host header
        } else {
            strBuf.append(msg.getLocalAddress().getHostName());
            strBuf.append(":");
            strBuf.append(msg.getLocalAddress().getPort());
        }
        strBuf.append(msg.getRequestURI().getPath());

        return strBuf.toString();
    }
}
