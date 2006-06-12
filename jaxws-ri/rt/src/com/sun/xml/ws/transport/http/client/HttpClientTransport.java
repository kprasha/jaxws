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

import com.sun.xml.ws.api.EndpointAddress;
import com.sun.xml.ws.api.message.Packet;
import static com.sun.xml.ws.client.BindingProviderProperties.*;
import com.sun.xml.ws.client.ClientTransportException;
import com.sun.xml.ws.client.BindingProviderProperties;
import com.sun.xml.ws.transport.Headers;
import com.sun.xml.ws.util.ByteArrayBuffer;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import static javax.xml.ws.BindingProvider.SESSION_MAINTAIN_PROPERTY;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;
import javax.xml.ws.WebServiceException;

/**
 * TODO: this class seems to be pointless. Just merge it with {@link HttpTransportPipe}.
 *
 * @author WS Development Team
 */
final class HttpClientTransport {

    private static String LAST_ENDPOINT = "";
    private static boolean redirect = true;
    private static final int START_REDIRECT_COUNT = 3;
    private static int redirectCount = START_REDIRECT_COUNT;

    /*package*/ int statusCode;
    private final Map<String, List<String>> reqHeaders;
    private Map<String, List<String>> respHeaders = null;

    private OutputStream outputStream;

    public HttpClientTransport(Packet packet, Map<String,List<String>> reqHeaders) {
        endpoint = packet.endpointAddress;
        context = packet;
        this.reqHeaders = reqHeaders;
    }

    /**
     * Prepare the stream for HTTP request
     */
    public OutputStream getOutput() {
        try {
            httpConnection = createHttpConnection();
            cookieJar = sendCookieAsNeeded();

            // how to incorporate redirect processing: message dispatcher does not seem to tbe right place
            if (!httpConnection.getRequestMethod().equalsIgnoreCase("GET"))
                outputStream = httpConnection.getOutputStream();
            //if use getOutputStream method set as "POST"
            //but for "Get" request no need to get outputStream
            connectForResponse();

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new ClientTransportException("http.client.failed",ex);
        }

        return outputStream;
    }

    public void closeOutput() throws IOException {
        if (outputStream != null) {
            outputStream.flush();
            outputStream.close();
        }
    }

    /**
     * Get the response from HTTP connection and prepare the input stream for response
     */
    public InputStream getInput() {
        // response processing

        InputStream in;
        try {
            in = readResponse();
        } catch (IOException e) {
            if (statusCode == HttpURLConnection.HTTP_NO_CONTENT
                || (isFailure
                && statusCode != HttpURLConnection.HTTP_INTERNAL_ERROR)) {
                try {
                    throw new ClientTransportException("http.status.code",
                        statusCode, httpConnection.getResponseMessage());
                } catch (IOException ex) {
                    throw new ClientTransportException("http.status.code",
                        statusCode, ex);
                }
            }
            throw new ClientTransportException("http.client.failed",
                e.getMessage());
        }
        httpConnection = null;

        return in;
    }

    public Map<String, List<String>> getHeaders() {
        if (respHeaders != null) {
            return respHeaders;
        }
        isFailure = checkResponseCode();

        respHeaders = collectResponseMimeHeaders();

        saveCookieAsNeeded(cookieJar);
        return respHeaders;
    }

    protected InputStream readResponse()
        throws IOException {
        InputStream contentIn =
            (isFailure
                ? httpConnection.getErrorStream()
                : httpConnection.getInputStream());

        ByteArrayBuffer bab = new ByteArrayBuffer();
        if (contentIn != null) { // is this really possible?
            bab.write(contentIn);
            bab.close();
        }

        int length =
            httpConnection.getContentLength() == -1
                ? bab.size()
                : httpConnection.getContentLength();

        return bab.newInputStream(0, length);
    }

    public Map<String, List<String>> collectResponseMimeHeaders() {
        Map<String, List<String>> headers = new Headers();
        headers.putAll(httpConnection.getHeaderFields());
        return headers;
    }

    protected void connectForResponse()
        throws IOException {

        httpConnection.connect();
    }

    /*
     * Will throw an exception instead of returning 'false' if there is no
     * return message to be processed (i.e., in the case of an UNAUTHORIZED
     * response from the servlet or 404 not found)
     */
    protected boolean checkResponseCode() {
        boolean isFailure = false;
        try {

            statusCode = httpConnection.getResponseCode();

            if ((httpConnection.getResponseCode()
                == HttpURLConnection.HTTP_INTERNAL_ERROR)) {
                isFailure = true;
                //added HTTP_ACCEPT for 1-way operations
            } else if (
                httpConnection.getResponseCode()
                    == HttpURLConnection.HTTP_UNAUTHORIZED) {

                // no soap message returned, so skip reading message and throw exception
                throw new ClientTransportException("http.client.unauthorized",
                    httpConnection.getResponseMessage());
            } else if (
                httpConnection.getResponseCode()
                    == HttpURLConnection.HTTP_NOT_FOUND) {

                // no message returned, so skip reading message and throw exception
                throw new ClientTransportException("http.not.found",
                    httpConnection.getResponseMessage());
            } else if (
                (statusCode == HttpURLConnection.HTTP_MOVED_TEMP) ||
                    (statusCode == HttpURLConnection.HTTP_MOVED_PERM)) {
                isFailure = true;

                if (!redirect || (redirectCount <= 0)) {
                    throw new ClientTransportException("http.status.code",
                            statusCode,getStatusMessage(httpConnection));
                }
            } else if (
                statusCode < 200 || (statusCode >= 303 && statusCode < 500)) {
                throw new ClientTransportException("http.status.code",
                    statusCode,
                    getStatusMessage(httpConnection));
            } else if (statusCode >= 500) {
                isFailure = true;
            }
        } catch (IOException e) {
            throw new WebServiceException(e);
            // on JDK1.3.1_01, we end up here, but then getResponseCode() succeeds!
//            if (httpConnection.getResponseCode()
//                    == HttpURLConnection.HTTP_INTERNAL_ERROR) {
//                isFailure = true;
//            } else {
//                throw e;
//            }
        }

        return isFailure;
    }

    protected String getStatusMessage(HttpURLConnection httpConnection)
        throws IOException {
        int statusCode = httpConnection.getResponseCode();
        String message = httpConnection.getResponseMessage();
        if (statusCode == HttpURLConnection.HTTP_CREATED
            || (statusCode >= HttpURLConnection.HTTP_MULT_CHOICE
            && statusCode != HttpURLConnection.HTTP_NOT_MODIFIED
            && statusCode < HttpURLConnection.HTTP_BAD_REQUEST)) {
            String location = httpConnection.getHeaderField("Location");
            if (location != null)
                message += " - Location: " + location;
        }
        return message;
    }

    protected CookieJar sendCookieAsNeeded() {
        Boolean shouldMaintainSessionProperty =
            (Boolean) context.invocationProperties.get(SESSION_MAINTAIN_PROPERTY);
        if (shouldMaintainSessionProperty == null) {
            return null;
        }
        if (shouldMaintainSessionProperty) {
            CookieJar cookieJar = (CookieJar) context.invocationProperties.get(HTTP_COOKIE_JAR);
            if (cookieJar == null) {
                cookieJar = new CookieJar();

                // need to store in binding's context so it is not lost
                context.proxy.getRequestContext().put(HTTP_COOKIE_JAR, cookieJar);
            }
            cookieJar.applyRelevantCookies(httpConnection);
            return cookieJar;
        } else {
            return null;
        }
    }

    protected void saveCookieAsNeeded(CookieJar cookieJar) {
        if (cookieJar != null) {
            cookieJar.recordAnyCookies(httpConnection);
        }
    }

    protected HttpURLConnection createHttpConnection()
            throws IOException {

        boolean verification = false;
        // does the client want client hostname verification by the service
        String verificationProperty =
            (String) context.otherProperties.get(HOSTNAME_VERIFICATION_PROPERTY);
        if (verificationProperty != null) {
            if (verificationProperty.equalsIgnoreCase("true"))
                verification = true;
        }

        // does the client want request redirection to occur
        String redirectProperty =
            (String) context.otherProperties.get(REDIRECT_REQUEST_PROPERTY);
        if (redirectProperty != null) {
            if (redirectProperty.equalsIgnoreCase("false"))
                redirect = false;
        }

        checkEndpoints();

        HttpURLConnection httpConnection = createConnection();

        if (!verification) {
            // for https hostname verification  - turn off by default
            if (httpConnection instanceof HttpsURLConnection) {
                ((HttpsURLConnection) httpConnection).setHostnameVerifier(new HttpClientVerifier());
            }
        }

        // allow interaction with the web page - user may have to supply
        // username, password id web page is accessed from web browser

        httpConnection.setAllowUserInteraction(true);
        // enable input, output streams
        httpConnection.setDoOutput(true);
        httpConnection.setDoInput(true);
        // the soap message is always sent as a Http POST
        // HTTP Get is disallowed by BP 1.0
        // needed for XML/HTTPBinding and SOAP12Binding
        // for xml/http binding other methods are allowed.
        // for Soap 1.2 "GET" is allowed.
        String method = "POST";
        /*
        String requestMethod = (String) context.get(MessageContext.HTTP_REQUEST_METHOD);
        if (context.get(BindingProviderProperties.BINDING_ID_PROPERTY).equals(HTTPBinding.HTTP_BINDING)){
            method = (requestMethod != null)?requestMethod:method;            
        } else if
            (context.get(BindingProviderProperties.BINDING_ID_PROPERTY).equals(SOAPBinding.SOAP12HTTP_BINDING) &&
            "GET".equalsIgnoreCase(requestMethod)) {
            method = (requestMethod != null)?requestMethod:method;
        }
         */
        httpConnection.setRequestMethod(method);
        
        Integer reqTimeout = (Integer)context.get(BindingProviderProperties.REQUEST_TIMEOUT);
        if (reqTimeout != null) {
            httpConnection.setReadTimeout(reqTimeout);
        }
        // set the properties on HttpURLConnection
        for (Map.Entry entry : reqHeaders.entrySet()) {
            httpConnection.addRequestProperty((String) entry.getKey(), ((List<String>) entry.getValue()).get(0));
        }

        return httpConnection;
    }

    private HttpURLConnection createConnection() throws IOException {
        return (HttpURLConnection) endpoint.openConnection();
    }

//    private void redirectRequest(HttpURLConnection httpConnection, SOAPMessageContext context) {
//        String redirectEndpoint = httpConnection.getHeaderField("Location");
//        if (redirectEndpoint != null) {
//            httpConnection.disconnect();
//            invoke(redirectEndpoint, context);
//        } else
//            System.out.println("redirection Failed");
//    }

    private boolean checkForRedirect(int statusCode) {
        return (((statusCode == 301) || (statusCode == 302)) && redirect && (redirectCount-- > 0));
    }

    private void checkEndpoints() {
        if (!LAST_ENDPOINT.equalsIgnoreCase(endpoint.toString())) {
            redirectCount = START_REDIRECT_COUNT;
            LAST_ENDPOINT = endpoint.toString();
        }
    }

    public HttpURLConnection getConnection() {
        return httpConnection;
    }

    // overide default SSL HttpClientVerifier to always return true
    // effectively overiding Hostname client verification when using SSL
    static class HttpClientVerifier implements HostnameVerifier {
        public boolean verify(String s, SSLSession sslSession) {
            return true;
        }
    }

    HttpURLConnection httpConnection = null;
    EndpointAddress endpoint = null;
    Packet context = null;
    CookieJar cookieJar = null;
    boolean isFailure = false;
}

