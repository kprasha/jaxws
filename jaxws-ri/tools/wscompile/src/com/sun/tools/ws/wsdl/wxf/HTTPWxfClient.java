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
package com.sun.tools.ws.wsdl.wxf;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

import java.net.HttpURLConnection;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;

import com.sun.tools.ws.wsdl.framework.ParseException;
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * A class for making ws transfer requests over http to retrieve
 * a WSDL.
 *
 * Big TODO: Pull the common http connection code out of the MEX and
 * WXF classes into a common utility class. Need to determine where
 * this will live after figuring out if these classes will live in
 * wsimport code or not.
 *
 * @author WS Development Team
 */
public class HTTPWxfClient {
    
    public Document getWSDLDocument(String address) {
        try {
            String request = getWxfWsdlRequest(address);
            System.out.println("Request message (this output is temporary)\n" +
                request + "\n");
            InputStream response = makeHTTPCall(request, address);
            return convertResponse(response);
        } catch (Exception e) {
            // todo: change exception handling once we know
            // where this code will end up living
            throw new ParseException(e);
        }
    }

    private String getWxfWsdlRequest(String address) {
        return "<s12:Envelope " +
            "xmlns:s12='http://www.w3.org/2003/05/soap-envelope' " +
            "xmlns:wsa='http://www.w3.org/2005/08/addressing' " +
            "xmlns:wxf='http://schemas.xmlsoap.org/ws/2004/09/transfer'>" +
            "<s12:Header>" +
            "<wsa:Action>" +
            "http://schemas.xmlsoap.org/ws/2004/09/transfer/Get" +
            "</wsa:Action>" +
            "<wsa:To>" + address + "</wsa:To>" +
            "</s12:Header>" +
            "<s12:Body/>" +
            "</s12:Envelope>";
    }

    private InputStream makeHTTPCall(String request, String address)
        throws Exception {
        URL url = new URL(address);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        //conn.setRequestProperty("SOAPAction",
          //  "http://schemas.xmlsoap.org/ws/2004/09/transfer/Get");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Transfer-Encoding", "8bit");
        conn.setRequestProperty("Content-Type",
            "application/soap+xml;charset=utf-8;action=\"http://schemas.xmlsoap.org/ws/2004/09/transfer/Get\"");
        
        Writer writer = new OutputStreamWriter(conn.getOutputStream());
        writer.write(request);
        writer.flush();
        
        try {
            return conn.getInputStream();
        } catch (IOException ioe) {
            outputErrorStream(conn);
            throw ioe;
        }
    }

    private Document convertResponse(InputStream stream) throws Exception {
        DocumentBuilderFactory factory =
            DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setErrorHandler(new ErrorHandler() {
            public void error(SAXParseException e) throws SAXException {}
            public void fatalError(SAXParseException e) throws SAXException {
                throw e;
            }
            public void warning(SAXParseException e) throws SAXException {
                throw e;
            }
        });
        Document responseDoc = builder.parse(stream);
        
        Node envelope = responseDoc.getFirstChild();
        Node body = envelope.getFirstChild().getNextSibling();
        Node wsdl = body.getFirstChild();
        while (!wsdl.getLocalName().equalsIgnoreCase("definitions")) {
            Node nextNode = wsdl.getFirstChild();
            if (nextNode == null) {
                throw new ParseException("no wsdl in response");
            }
            wsdl = nextNode;
        }
        responseDoc.replaceChild(wsdl, envelope);
        if (true) { // temporary
            javax.xml.transform.Transformer xFormer =
                javax.xml.transform.TransformerFactory.newInstance().
                newTransformer();
            xFormer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT,
                "yes");
            System.err.println("------------------------------");
            xFormer.transform(new javax.xml.transform.dom.DOMSource(responseDoc),
                new javax.xml.transform.stream.StreamResult(System.err));
            System.err.println("\n------------------------------");
        }
        
        return responseDoc;
    }
    
    private void outputErrorStream(HttpURLConnection conn) {
        InputStream error = conn.getErrorStream();
        if (error != null) {
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(error));
            try {
                System.err.println("Error returned from server:");
                String line = reader.readLine();
                while (line != null) {
                    System.err.println(line);
                    line = reader.readLine();
                }
            } catch (IOException ioe) {
                // This exception has no impact on wsimport -- the connection
                // has already failed at this point.
                System.err.println(
                    "Exception ignored while reading error stream:");
                ioe.printStackTrace();
            }
        }
    }
    
}
