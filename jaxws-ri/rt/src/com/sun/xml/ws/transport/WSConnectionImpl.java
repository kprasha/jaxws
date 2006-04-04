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

package com.sun.xml.ws.transport;

import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.server.WSConnection;
import com.sun.xml.ws.client.ClientTransportException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract class for WSConnection. All client-side and server-side
 * transports should extend this class and override appropriate methods.
 *
 * @author WS Development Team
 */
public abstract class WSConnectionImpl implements WSConnection {
    private Map<String, List<String>> reqHeaders = null;
    private Map<String, List<String>> rspHeaders = null;
    public OutputStream outputStream = null;
    public InputStream inputStream = null;
    protected int statusCode;

    /** Creates a new instance of WSConnectionImpl */
    public WSConnectionImpl () {
    }

    public int getStatus () {
        return statusCode;
    }

    public void setStatus (int statusCode) {
        this.statusCode = statusCode;
    }

    public Map<String, List<String>> getResponseHeaders() {
        return rspHeaders;
    }

    public void setRequestHeaders(Map<String, List<String>> reqHeaders) {
        this.reqHeaders = reqHeaders;
    }

    /**
     * @return outputStream
     * 
     * Returns the OutputStream on which the outbound message is written.
     * Any stream or connection initialization, pre-processing is done here.
     */
    public OutputStream getOutput() {
        return outputStream;
    }

    /**
     * @return inputStream
     *
     * Returns the InputStream on which the inbound message is received.
     * Any post-processing of message is done here.
     */
    public InputStream getInput() {
        return inputStream;
    }

    public void wrapUpRequestPacket(Packet p) {
        // noop
    }

    public Map<String, List<String>> getRequestHeaders () {
        return reqHeaders;
    }

    // default implementation
    public String getRequestHeader(String headerName) {
        List<String> values = getRequestHeaders().get(headerName);
        if(values==null || values.isEmpty())
            return null;
        else
            return values.get(0);
    }

    public void setResponseHeaders(Map<String, List<String>> headers) {
        this.rspHeaders = headers;
    }

    /**
     * Write connection headers in HTTP syntax using \r\n as a
     * separator.
     */
    public void writeHeaders(OutputStream os) {
        try {
            byte[] newLine = "\r\n".getBytes("us-ascii");

            // Write all headers ala HTTP (only first list entry serialized)
            for (String header : rspHeaders.keySet()) {
                os.write((header + ":" +
                    rspHeaders.get(header).get(0)).getBytes("us-ascii"));
                os.write(newLine);
            }

            // Write empty line as in HTTP
            os.write(newLine);
        }
        catch (Exception ex) {
            throw new ClientTransportException("local.client.failed",ex);
        }
    }

    /**
     * Read and consume connection headers in HTTP syntax using 
     * \r\n as a separator.
     */
    public void readHeaders(InputStream is) {
        try {
            int c1, c2;
            StringBuffer line = new StringBuffer();

            if (reqHeaders == null) {
                reqHeaders = new HashMap<String, List<String>>();
            } else {
                reqHeaders.clear();
            }

            // Read headers until finding a \r\n line
            while ((c1 = is.read()) != -1) {
                if (c1 == '\r') {
                    c2 = is.read();
                    assert c2 != -1;

                    if (c2 == '\n') {
                        String s = line.toString();
                        if (s.length() == 0) {
                            break;  // found \r\n line
                        }
                        else {
                            int k  = s.indexOf(':');
                            assert k > 0;
                            ArrayList<String> value = new ArrayList<String>();
                            value.add(s.substring(k + 1));
                            reqHeaders.put(s.substring(0, k), value);
                            line.setLength(0);      // clear line buffer
                        }
                    }
                    else {
                        line.append((char) c1).append((char) c2);
                    }
                }
                else {
                    line.append((char) c1);
                }
            }
        }
        catch (Exception ex) {
            throw new ClientTransportException("local.client.failed",ex);
        }
    }

    public void closeOutput() {
        try {
            if (outputStream != null) {
                outputStream.flush();
                outputStream.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void closeInput() {
    }

    public void close() {

    }
}
