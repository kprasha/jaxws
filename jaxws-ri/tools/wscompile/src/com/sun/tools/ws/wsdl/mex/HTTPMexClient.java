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
package com.sun.tools.ws.wsdl.mex;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import java.net.HttpURLConnection;
import java.net.URL;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.sun.tools.ws.wsdl.framework.ParseException;

/**
 * A class for making mex requests over http to retrieve
 * a WSDL.
 *
 * Big TODO: Pull the common http connection code out of the MEX and
 * WXF classes into a common utility class. Need to determine where
 * this will live after figuring out if these classes will live in
 * wsimport code or not.
 *
 * @author WS Development Team
 */
public class HTTPMexClient {
    
    private JAXBContext jaxbContext;

    public Document getWSDLDocument(String address) {
        try {
            String request = getMexWsdlRequest(address);
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

    private String getMexWsdlRequest(String address) {
        return "<?xml version=\"1.0\"?>" +
            "<soapenv:Envelope " +
            "xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope\" " +
            "xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" " +
            "xmlns:wsx=\"http://schemas.xmlsoap.org/ws/2004/09/mex\">" +
            "<soapenv:Header><wsa:Action>" +
            "http://schemas.xmlsoap.org/ws/2004/09/mex/GetMetadata/Request" +
            "</wsa:Action><wsa:MessageID>urn:GetMetadata</wsa:MessageID>" +
            "<wsa:ReplyTo><wsa:Address>" +
            "http://www.w3.org/2005/08/addressing/anonymous" +
            "</wsa:Address></wsa:ReplyTo><wsa:To>" +
            address +
            "</wsa:To></soapenv:Header><soapenv:Body>" +
            "<wsx:GetMetadata/>" + // empty request maps to wsdl
            "</soapenv:Body></soapenv:Envelope>";
    }

    private InputStream makeHTTPCall(String request, String address)
        throws Exception {
        URL url = new URL(address);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("SOAPAction",
            "http://schemas.xmlsoap.org/ws/2004/09/mex/GetMetadata/Request");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"");
        
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
        createJAXBContext();
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLStreamReader reader = factory.createXMLStreamReader(stream);
        
        int state = 0;
        do {
            state = reader.next();
        } while (state != reader.START_ELEMENT ||
            !reader.getLocalName().equalsIgnoreCase("metadata"));
        
        Unmarshaller uMarshaller = jaxbContext.createUnmarshaller();
        Metadata mexResponse = (Metadata) uMarshaller.unmarshal(reader);
        MetadataSection wsdlSection = mexResponse.getMetadataSection().get(0);
        Node wsdlNode = (Node) wsdlSection.getAny().get(0);
        return wsdlNode.getOwnerDocument();
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
    
    private void createJAXBContext() throws JAXBException {
        if (jaxbContext == null) {
            jaxbContext = JAXBContext.newInstance(
                com.sun.tools.ws.wsdl.mex.ObjectFactory.class);
        }
    }
}
