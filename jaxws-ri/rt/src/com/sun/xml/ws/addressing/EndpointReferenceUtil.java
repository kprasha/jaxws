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

import com.sun.istack.NotNull;
import com.sun.istack.Nullable;
import com.sun.xml.messaging.saaj.util.ByteOutputStream;
import com.sun.xml.ws.addressing.v200408.MemberSubmissionAddressingConstants;
import com.sun.xml.ws.api.addressing.MemberSubmissionEndpointReference;
import com.sun.xml.ws.streaming.XMLStreamWriterFactory;
import com.sun.xml.ws.util.DOMUtil;
import com.sun.xml.ws.util.xml.XmlUtil;
import com.sun.xml.ws.wsdl.parser.WSDLConstants;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.EndpointReference;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.wsaddressing.W3CEndpointReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
                //TODO: write ReferenceProperties
                //TODO: write ReferenceParameters
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
            if(portType != null){
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
            }

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

    /**
     * Gives the EPR based on the clazz. It may need to perform tranformation from
     * W3C EPR to MS EPR or vise-versa.
     */
    public static <T extends EndpointReference> T transform(Class<T> clazz, @NotNull EndpointReference epr){
        assert epr != null;
        if(clazz.isAssignableFrom(W3CEndpointReference.class)){
            if(epr instanceof W3CEndpointReference){
                return (T)epr;
            }else if(epr instanceof MemberSubmissionEndpointReference){
                return (T)toW3CEpr((MemberSubmissionEndpointReference)epr);
            }
        }else if(clazz.isAssignableFrom(MemberSubmissionEndpointReference.class)){
            if(epr instanceof W3CEndpointReference){
                return (T)toMSEpr((W3CEndpointReference)epr);
            }else if(epr instanceof MemberSubmissionEndpointReference){
                return (T)epr;
            }
        }

        //This must be an EPR that we dont know
        throw new WebServiceException("Unknwon EndpointReference: "+epr.getClass());
    }

    //TODO: bit of redundency on writes of w3c epr, should modularize it
    private static W3CEndpointReference toW3CEpr(MemberSubmissionEndpointReference msEpr) {
        final ByteOutputStream bos = new ByteOutputStream();
        XMLStreamWriter writer = XMLStreamWriterFactory.createXMLStreamWriter(bos);
        try {
            writer.writeStartDocument();
            writer.writeStartElement(W3CAddressingConstants.WSA_NAMESPACE_PREFIX,
                    "EndpointReference", W3CAddressingConstants.WSA_NAMESPACE_NAME);
            writer.writeNamespace(W3CAddressingConstants.WSA_NAMESPACE_PREFIX,
                    W3CAddressingConstants.WSA_NAMESPACE_NAME);
            //write wsa:Address
            writer.writeStartElement(W3CAddressingConstants.WSA_NAMESPACE_PREFIX,
                    W3CAddressingConstants.WSA_ADDRESS_NAME, W3CAddressingConstants.WSA_NAMESPACE_NAME);
            writer.writeCharacters(msEpr.addr.uri);
            writer.writeEndElement();
            //TODO: write extension attributes on wsa:Address


            //write ReferenceProperties
            if(msEpr.referenceProperties != null && msEpr.referenceProperties.elements.size() > 0){
                writer.writeStartElement(W3CAddressingConstants.WSA_NAMESPACE_PREFIX, W3CAddressingConstants.WSA_REFERENCEPARAMETERS_NAME, W3CAddressingConstants.WSA_NAMESPACE_NAME);
                for(Element e : msEpr.referenceProperties.elements){
                    DOMUtil.serializeNode(e, writer);
                }
                writer.writeEndElement();
            }

            //write referenceParameters
            if(msEpr.referenceParameters != null && msEpr.referenceParameters.elements.size() > 0){
                writer.writeStartElement(W3CAddressingConstants.WSA_NAMESPACE_PREFIX, W3CAddressingConstants.WSA_METADATA_NAME, W3CAddressingConstants.WSA_NAMESPACE_NAME);
                //Write Interface info
                if(msEpr.portTypeName != null){
                    writer.writeStartElement(MemberSubmissionAddressingConstants.WSA_NAMESPACE_PREFIX,
                            MemberSubmissionAddressingConstants.WSA_PORTTYPE_NAME,
                            MemberSubmissionAddressingConstants.WSA_NAMESPACE_NAME);
                    String portTypePrefix = fixNull(msEpr.portTypeName.name.getPrefix());
                    writer.writeNamespace(portTypePrefix, msEpr.portTypeName.name.getNamespaceURI());
                    if(!portTypePrefix.equals(""))
                        writer.writeCharacters(msEpr.portTypeName.name.getLocalPart());
                    else
                        writer.writeCharacters(portTypePrefix + ":" + msEpr.portTypeName.name.getLocalPart());
                    writer.writeEndElement();
                }
                if(msEpr.serviceName != null){
                    //Write service and Port info
                    writer.writeStartElement(MemberSubmissionAddressingConstants.WSA_NAMESPACE_PREFIX,
                            MemberSubmissionAddressingConstants.WSA_SERVICENAME_NAME,
                            MemberSubmissionAddressingConstants.WSA_NAMESPACE_NAME);
                    String servicePrefix = fixNull(msEpr.serviceName.name.getPrefix());
                    if(msEpr.serviceName.portName != null)
                        writer.writeAttribute(MemberSubmissionAddressingConstants.WSA_PORTNAME_NAME,
                                msEpr.serviceName.portName);

                    writer.writeNamespace(servicePrefix, msEpr.serviceName.name.getNamespaceURI());
                    if(servicePrefix.length() > 0)
                        writer.writeCharacters(servicePrefix + ":" + msEpr.serviceName.name.getLocalPart());
                    else
                        writer.writeCharacters(msEpr.serviceName.name.getLocalPart());
                    writer.writeEndElement();
                }

                for(Element e : msEpr.referenceParameters.elements){
                    DOMUtil.serializeNode(e, writer);
                }

                //</Metadata>
                writer.writeEndElement();
            }

            //write extension elements
            if((msEpr.elements != null) && (msEpr.elements.elements.size() > 0)){
                for(Element e : msEpr.elements.elements){
                    DOMUtil.serializeNode(e, writer);
                }
            }

            //TODO:write extension attributes

            //</EndpointReference>
            writer.writeEndElement();
            writer.writeEndDocument();
            writer.flush();
        } catch (XMLStreamException e) {
            throw new WebServiceException(e);
        }
        return new W3CEndpointReference(new StreamSource(bos.newInputStream()));
    }

    private static MemberSubmissionEndpointReference toMSEpr(W3CEndpointReference w3cEpr){
        DOMResult result = new DOMResult();
        w3cEpr.writeTo(result);
        Node eprNode = result.getNode();
        Element e = DOMUtil.getFirstElementChild(eprNode);
        if(e == null)
            return null;

        MemberSubmissionEndpointReference msEpr = new MemberSubmissionEndpointReference();

        NodeList nodes = e.getChildNodes();
        for(int i=0; i < nodes.getLength(); i++){
            if(nodes.item(i).getNodeType() == Node.ELEMENT_NODE){
                Element child = (Element)nodes.item(i);
                if(child.getNamespaceURI().equals(W3CAddressingConstants.WSA_NAMESPACE_NAME) &&
                        child.getLocalName().equals(W3CAddressingConstants.WSA_ADDRESS_NAME)){
                    if(msEpr.addr == null)
                        msEpr.addr = new MemberSubmissionEndpointReference.Address();
                    msEpr.addr.uri = XmlUtil.getTextForNode(child);

                    //now add the attribute extensions
                    msEpr.addr.attributes = getAttributes(child);
                }else if(child.getNamespaceURI().equals(W3CAddressingConstants.WSA_NAMESPACE_NAME) &&
                        child.getLocalName().equals(W3CAddressingConstants.WSA_REFERENCEPARAMETERS_NAME)){
                        NodeList refParams = child.getChildNodes();
                        for(int j=0; j < refParams.getLength(); j++){
                            if(refParams.item(j).getNodeType() == Node.ELEMENT_NODE){
                                if(msEpr.referenceProperties == null)
                                    msEpr.referenceProperties.elements = new ArrayList<Element>();
                                msEpr.referenceProperties.elements.add((Element)refParams.item(i));
                            }
                        }
                }else if (child.getNamespaceURI().equals(W3CAddressingConstants.WSA_NAMESPACE_NAME) &&
                        child.getLocalName().equals(W3CAddressingConstants.WSA_METADATA_NAME)) {
                    NodeList metadata = child.getChildNodes();
                    for (int j = 0; j < metadata.getLength(); j++) {
                        Node node = metadata.item(j);
                        if (node.getNodeType() != Node.ELEMENT_NODE)
                            continue;

                        Element elm = (Element) node;
                        if (elm.getNamespaceURI().equals(W3CAddressingConstants.WSA_NAMESPACE_WSDL_NAME) &&
                                elm.getLocalName().equals(W3CAddressingConstants.WSAW_SERVICENAME_NAME)) {
                            msEpr.serviceName = new MemberSubmissionEndpointReference.ServiceNameType();
                            msEpr.serviceName.portName = elm.getAttribute(W3CAddressingConstants.WSAW_ENDPOINTNAME_NAME);

                            String service = elm.getTextContent();
                            String prefix = XmlUtil.getPrefix(service);
                            String name = XmlUtil.getLocalPart(service);

                            //if there is no service name then its not a valid EPR but lets continue as its optional anyway
                            if (name == null)
                                continue;

                            if (prefix != null) {
                                String ns = elm.lookupNamespaceURI(prefix);
                                if (ns != null)
                                    msEpr.serviceName.name = new QName(ns, name, prefix);
                            } else {
                                msEpr.serviceName.name = new QName(null, name);
                            }
                            msEpr.serviceName.attributes = getAttributes(elm);
                        } else if (elm.getNamespaceURI().equals(W3CAddressingConstants.WSA_NAMESPACE_WSDL_NAME) &&
                                elm.getLocalName().equals(W3CAddressingConstants.WSAW_INTERFACENAME_NAME)) {
                            msEpr.portTypeName = new MemberSubmissionEndpointReference.AttributedQName();

                            String portType = elm.getTextContent();
                            String prefix = XmlUtil.getPrefix(portType);
                            String name = XmlUtil.getLocalPart(portType);

                            //if there is no portType name then its not a valid EPR but lets continue as its optional anyway
                            if (name == null)
                                continue;

                            if (prefix != null) {
                                String ns = elm.lookupNamespaceURI(prefix);
                                if (ns != null)
                                    msEpr.portTypeName.name = new QName(ns, name, prefix);
                            } else {
                                msEpr.portTypeName.name = new QName(null, name);
                            }
                            msEpr.portTypeName.attributes = getAttributes(elm);
                        } else {
                            //its extensions in META-DATA and should be copied to ReferenceProperties in MS EPR
                            if (msEpr.referenceParameters == null)
                                msEpr.referenceParameters.elements = new ArrayList<Element>();
                            msEpr.referenceParameters.elements.add(elm);
                        }
                    }
                }else{
                    //its extensions
                    if(msEpr.elements == null)
                        msEpr.elements.elements = new ArrayList<Element>();
                    msEpr.elements.elements.add((Element)child);

                }
            }else if(nodes.item(i).getNodeType() == Node.ATTRIBUTE_NODE){
                Node n = nodes.item(i);
                if(msEpr.attributes == null){
                    msEpr.attributes = new HashMap<QName, String>();
                    String prefix = fixNull(n.getPrefix());
                    String ns = fixNull(n.getNamespaceURI());
                    String localName = n.getLocalName();
                    msEpr.attributes.put(new QName(ns, localName, prefix), n.getNodeValue());
                }
            }
        }

        return msEpr;
    }

    private static Map<QName, String> getAttributes(Node node){
        Map<QName, String> attribs = null;

        NamedNodeMap nm = node.getAttributes();
        for(int i=0;i<nm.getLength();i++){
            if(attribs == null)
                 attribs = new HashMap<QName, String>();
            Node n = nm.item(i);
            String prefix = fixNull(n.getPrefix());
            String ns = fixNull(n.getNamespaceURI());
            String localName = n.getLocalName();
            //exclude some attributes
            if(!localName.equals(W3CAddressingConstants.WSAW_ENDPOINTNAME_NAME))
                attribs.put(new QName(ns, localName, prefix), n.getNodeValue());
        }
        return attribs;
    }

    private static
    @NotNull
    String fixNull(@Nullable String s) {
        if (s == null) return "";
        else return s;
    }
}

