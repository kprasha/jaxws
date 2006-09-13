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
package com.sun.xml.ws.addressing;

import com.sun.xml.messaging.saaj.util.ByteOutputStream;
import com.sun.xml.ws.streaming.XMLStreamWriterFactory;
import com.sun.xml.ws.api.addressing.MemberSubmissionEndpointReference;
import com.sun.xml.ws.addressing.v200408.MemberSubmissionAddressingConstants;
import com.sun.xml.ws.wsdl.parser.WSDLConstants;

import javax.xml.ws.EndpointReference;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.w3caddressing.W3CEndpointReference;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.namespace.QName;
import java.io.IOException;

/**
 * @author Rama Pulavarthi
 */

public class EndpointReferenceUtil {

    public static <T extends EndpointReference> T getEndpointReference(Class<T> clazz, String address) {
        return getEndpointReference(clazz, address, null, null, null);
    }

    public static <T extends EndpointReference> T getEndpointReference(Class<T> clazz, String address,
                                                                       QName service,
                                                                       String port,
                                                                       QName portType) {
        if (clazz.isAssignableFrom(W3CEndpointReference.class)) {
            final ByteOutputStream bos = new ByteOutputStream();
            XMLStreamWriter writer = XMLStreamWriterFactory.createXMLStreamWriter(bos);
            try {
                writer.writeStartDocument();
                writer.writeStartElement(W3CAddressingConstants.WSA_NAMESPACE_PREFIX,
                        "EndpointReference", W3CAddressingConstants.WSA_NAMESPACE_NAME);
                writer.writeNamespace(W3CAddressingConstants.WSA_NAMESPACE_PREFIX,
                        W3CAddressingConstants.WSA_NAMESPACE_NAME);
                writer.writeStartElement(W3CAddressingConstants.WSA_NAMESPACE_PREFIX,
                        W3CAddressingConstants.WSA_ADDRESS_NAME, W3CAddressingConstants.WSA_NAMESPACE_NAME);
                writer.writeCharacters(address);
                writer.writeEndElement();
                writeW3CMetaData(writer, address, service, port, portType);
                writer.writeEndElement();
                writer.writeEndDocument();
                writer.flush();
            } catch (XMLStreamException e) {
                throw new WebServiceException(e);
            }
            //System.out.println(bos.toString());
            return (T) new W3CEndpointReference(new StreamSource(bos.newInputStream()));
        } else if (clazz.isAssignableFrom(MemberSubmissionEndpointReference.class)) {
            final ByteOutputStream bos = new ByteOutputStream();
            XMLStreamWriter writer = XMLStreamWriterFactory.createXMLStreamWriter(bos);
            try {
                writer.writeStartDocument();
                writer.writeStartElement(MemberSubmissionAddressingConstants.WSA_NAMESPACE_PREFIX,
                        "EndpointReference", MemberSubmissionAddressingConstants.WSA_NAMESPACE_NAME);
                writer.writeNamespace(MemberSubmissionAddressingConstants.WSA_NAMESPACE_PREFIX,
                        MemberSubmissionAddressingConstants.WSA_NAMESPACE_NAME);
                writer.writeStartElement(MemberSubmissionAddressingConstants.WSA_NAMESPACE_PREFIX,
                        MemberSubmissionAddressingConstants.WSA_ADDRESS_NAME,
                        MemberSubmissionAddressingConstants.WSA_NAMESPACE_NAME);
                writer.writeCharacters(address);
                writer.writeEndElement();
                writeMSMetaData(writer, service, port, portType);
                writer.writeEndElement();
                writer.writeEndDocument();
                writer.flush();
            } catch (XMLStreamException e) {
                throw new WebServiceException(e);
            }
            //System.out.println(bos.toString());
            return (T) new MemberSubmissionEndpointReference(new StreamSource(bos.newInputStream()));
        } else {
            throw new WebServiceException(clazz + "is not a recognizable EndpointReference");
        }
    }

    private static void writeW3CMetaData(XMLStreamWriter writer, String eprAddress,
                                         QName service,
                                         String port,
                                         QName portType) throws XMLStreamException {
        if (port != null) {
            writer.writeStartElement(W3CAddressingConstants.WSA_NAMESPACE_PREFIX,
                    W3CAddressingConstants.WSA_METADATA_NAME, W3CAddressingConstants.WSA_NAMESPACE_NAME);
            writer.writeNamespace(W3CAddressingConstants.WSA_NAMESPACE_WSDL_PREFIX,
                    W3CAddressingConstants.WSA_NAMESPACE_WSDL_NAME);

            //Write Interface info
            writer.writeStartElement(W3CAddressingConstants.WSA_NAMESPACE_WSDL_PREFIX,
                    W3CAddressingConstants.WSAW_INTERFACENAME_NAME,
                    W3CAddressingConstants.WSA_NAMESPACE_WSDL_NAME);
            String portTypePrefix = portType.getPrefix();
            if (portTypePrefix == null || portTypePrefix.equals("")) {
                //TODO check prefix again
                portTypePrefix = "wsns";
            }
            writer.writeNamespace(portTypePrefix, portType.getNamespaceURI());
            writer.writeCharacters(portTypePrefix + ":" + portType.getLocalPart());
            writer.writeEndElement();

            //Write service and Port info
            writer.writeStartElement(W3CAddressingConstants.WSA_NAMESPACE_WSDL_PREFIX,
                    W3CAddressingConstants.WSAW_SERVICENAME_NAME,
                    W3CAddressingConstants.WSA_NAMESPACE_WSDL_NAME);
            String servicePrefix = service.getPrefix();
            if (servicePrefix == null || servicePrefix.equals("")) {
                //TODO check prefix again
                servicePrefix = "wsns";
            }
            writer.writeAttribute(W3CAddressingConstants.WSAW_ENDPOINTNAME_NAME, port);
            writer.writeNamespace(servicePrefix, service.getNamespaceURI());
            writer.writeCharacters(servicePrefix + ":" + service.getLocalPart());
            writer.writeEndElement();
            //Inline the wsdl
            writer.writeStartElement(WSDLConstants.PREFIX_NS_WSDL,
                    WSDLConstants.QNAME_DEFINITIONS.getLocalPart(),
                    WSDLConstants.NS_WSDL);
            writer.writeNamespace(WSDLConstants.PREFIX_NS_WSDL, WSDLConstants.NS_WSDL);
            writer.writeStartElement(WSDLConstants.PREFIX_NS_WSDL,
                    WSDLConstants.QNAME_IMPORT.getLocalPart(),
                    WSDLConstants.NS_WSDL);
            writer.writeAttribute("namespace", service.getNamespaceURI());
            writer.writeAttribute("location", eprAddress + "?wsdl");
            writer.writeEndElement();
            writer.writeEndElement();

            writer.writeEndElement();
        }
    }

    private static void writeMSMetaData(XMLStreamWriter writer,
                                        QName service,
                                        String port,
                                        QName portType) throws XMLStreamException {
        //Write Interface info
        writer.writeStartElement(MemberSubmissionAddressingConstants.WSA_NAMESPACE_PREFIX,
                MemberSubmissionAddressingConstants.WSA_PORTTYPE_NAME,
                MemberSubmissionAddressingConstants.WSA_NAMESPACE_NAME);
        String portTypePrefix = portType.getPrefix();
        if (portTypePrefix == null || portTypePrefix.equals("")) {
            //TODO check prefix again
            portTypePrefix = "wsns";
        }
        writer.writeNamespace(portTypePrefix, portType.getNamespaceURI());
        writer.writeCharacters(portTypePrefix + ":" + portType.getLocalPart());
        writer.writeEndElement();

        //Write service and Port info
        writer.writeStartElement(MemberSubmissionAddressingConstants.WSA_NAMESPACE_PREFIX,
                MemberSubmissionAddressingConstants.WSA_SERVICENAME_NAME,
                MemberSubmissionAddressingConstants.WSA_NAMESPACE_NAME);
        String servicePrefix = service.getPrefix();
        if (servicePrefix == null || servicePrefix.equals("")) {
            //TODO check prefix again
            servicePrefix = "wsns";
        }
        writer.writeAttribute(MemberSubmissionAddressingConstants.WSA_PORTNAME_NAME,
                port);
        writer.writeNamespace(servicePrefix, service.getNamespaceURI());
        writer.writeCharacters(servicePrefix + ":" + service.getLocalPart());
        writer.writeEndElement();
    }
}

