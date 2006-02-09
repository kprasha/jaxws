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
package com.sun.xml.ws.server;

import com.sun.xml.ws.api.WSEndpoint;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.server.DocumentAddressResolver;
import com.sun.xml.ws.api.server.SDDocument;
import com.sun.xml.ws.util.xml.XMLStreamReaderToXMLStreamWriter;
import com.sun.xml.ws.wsdl.parser.WSDLConstants;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

/**
 * Patches WSDL with the correct endpoint address and the relative paths
 * to other documents.
 *
 * @author Jitu
 * @author Kohsuke Kawaguchi
 */
final class WSDLPatcher extends XMLStreamReaderToXMLStreamWriter {
    
    private static final String NS_XSD = "http://www.w3.org/2001/XMLSchema";
    private static final QName SCHEMA_INCLUDE_QNAME = new QName(NS_XSD, "include");
    private static final QName SCHEMA_IMPORT_QNAME = new QName(NS_XSD, "import");

    private static final Logger logger = Logger.getLogger(
            com.sun.xml.ws.util.Constants.LoggingDomain + ".wsdl.patcher");

    /**
     * {@link WSEndpoint} that owns the WSDL we are patching right now.
     */
    private final WSEndpointImpl<?> endpoint;

    /**
     * Document that is being patched.
     */
    private final SDDocumentImpl current;
    private String serviceAddress;

    private final DocumentAddressResolver resolver;


    //
    // fields accumulated as we parse through documents
    //
    private String targetNamespace;
    private QName serviceName;
    private QName portName;

    /**
     * Creates a {@link WSDLPatcher} for patching WSDL.
     *
     * @param endpoint
     *      The endpoint that we are patchinig WSDL for. This object is consulted
     *      to check other {@link SDDocument}s. Must not be null.
     * @param current
     *      The document that we are patching. Must not be null.
     * @param serviceAddress
     *      The address of the service, such as "http://host:port/context/"
     *      Must not be null.
     * @param resolver
     *      Consulted to generate references among  {@link SDDocument}s.
     *      Must not be null.
     */
    public WSDLPatcher(WSEndpointImpl<?> endpoint, SDDocumentImpl current, String serviceAddress, DocumentAddressResolver resolver) {
        this.endpoint = endpoint;
        this.current = current;
        this.serviceAddress = serviceAddress;
        this.resolver = resolver;
    }

    @Override
    protected void handleAttribute(int i) throws XMLStreamException {
        QName name = in.getName();
        String attLocalName = in.getAttributeLocalName(i);

        if((name.equals(SCHEMA_INCLUDE_QNAME) && attLocalName.equals("schemaLocation"))
        || (name.equals(SCHEMA_IMPORT_QNAME)  && attLocalName.equals("schemaLocation"))
        || (name.equals(WSDLConstants.QNAME_IMPORT)  && attLocalName.equals("location"))) {
            // patch this attribute value.

            String relPath = in.getAttributeValue(i);
            //if (isPatchable(relPath)) {
                String absPath = getPatchedImportLocation(relPath);
                if (absPath == null) {
                    //logger.warning("Couldn't fix the relative location:"+relPath);
                    // Not patching
                    super.handleAttribute(i);
                    return;
                }

                logger.fine("Fixing the relative location:"+relPath
                        +" with absolute location:"+absPath);
            writeAttribute(i, absPath);
            return;
        }

        if (name.equals(WSDLConstants.NS_SOAP_BINDING_ADDRESS) ||
            name.equals(WSDLConstants.NS_SOAP12_BINDING_ADDRESS)) {

            if(attLocalName.equals("location")) {
                String value = getAddressLocation();
                if (value != null) {
                    logger.fine("Fixing service:"+serviceName+ " port:"+portName
                            + " address with "+value);
                    writeAttribute(i, value);
                    return;
                }
            }
        }

        super.handleAttribute(i);
    }

    /**
     * Writes out an {@code i}-th attribute but with a different value.
     */
    private void writeAttribute(int i, String value) throws XMLStreamException {
        out.writeAttribute(
            in.getAttributePrefix(i),
            in.getAttributeNamespace(i),
            in.getAttributeLocalName(i),
            value
        );
    }

    @Override
    protected void handleStartElement() throws XMLStreamException {
        QName name = in.getName();

        if (name.equals(WSDLConstants.QNAME_DEFINITIONS)) {
            String value = in.getAttributeValue("","targetNamespace");
            if (value != null) {
                targetNamespace = value;
            }
        } else if (name.equals(WSDLConstants.QNAME_SERVICE)) {
            String value = in.getAttributeValue("","name");
            if (value != null) {
                serviceName = new QName(targetNamespace, value);
            }
        } else if (name.equals(WSDLConstants.QNAME_PORT)) {
            String value = in.getAttributeValue("","name");
            if (value != null) {
                portName = new QName(targetNamespace,value);
            }
        }
        super.handleStartElement();
    }

    /*
     * return patchedlocation  null if we don't how to patch this location 
     */
    private String getPatchedImportLocation(String relPath) {
        try {
            URL ref = new URL(current.getURL(), relPath);
            SDDocument refDoc = endpoint.getServiceDefinition().getBySystemId(ref);
            if(refDoc==null)
                return null;

            return resolver.getRelativeAddressFor(current,refDoc);
        } catch(MalformedURLException mue) {
            return null;
        }
    }

    /**
     * For the given service, port names it matches the correct endpoint and
     * reutrns its endpoint address
     */
    private String getAddressLocation() {
        WSDLPort port = endpoint.getPort();
        if(!port.getName().equals(portName))
            return null;

        if(!port.getOwner().getName().equals(serviceName))
            return null;

        return serviceAddress;
    }
}
    

