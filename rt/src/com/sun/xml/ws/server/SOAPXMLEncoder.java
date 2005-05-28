/*
 * $Id: SOAPXMLEncoder.java,v 1.5 2005-05-28 01:10:12 spericas Exp $
 */

/*
* Copyright (c) 2004 Sun Microsystems, Inc.
* All rights reserved.
*/
package com.sun.xml.ws.server;

import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLStreamException;

import com.sun.pept.ept.MessageInfo;
import com.sun.xml.bind.api.BridgeContext;
import com.sun.xml.messaging.saaj.util.ByteInputStream;
import com.sun.xml.ws.client.BindingProviderProperties;
import com.sun.xml.ws.encoding.jaxb.JAXBBridgeInfo;
import com.sun.xml.ws.encoding.soap.SOAPConstants;
import com.sun.xml.ws.encoding.soap.SOAPEncoder;
import com.sun.xml.ws.encoding.soap.internal.InternalMessage;
import com.sun.xml.ws.encoding.soap.message.SOAPFaultInfo;
import com.sun.xml.ws.encoding.soap.message.SOAPMessageContext;
import com.sun.xml.ws.encoding.soap.streaming.SOAPNamespaceConstants;
import com.sun.xml.ws.encoding.JAXWSAttachmentMarshaller;
import com.sun.xml.ws.streaming.XMLStreamWriterFactory;
import com.sun.xml.ws.streaming.DOMStreamReader;
import com.sun.xml.ws.util.exception.LocalizableExceptionAdapter;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.soap.Detail;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPMessage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

/**
 * @author JAX-RPC RI Development Team
 */
public class SOAPXMLEncoder extends SOAPEncoder {

    protected static final String FAULTCODE_NAME   = "faultcode";
    protected static final String FAULTSTRING_NAME = "faultstring";
    protected static final String FAULTACTOR_NAME  = "faultactor";
    protected static final String DETAIL_NAME      = "detail";
    
    public SOAPXMLEncoder() {
    }

    public SOAPMessage toSOAPMessage(InternalMessage response, MessageInfo messageInfo) {
        XMLStreamWriter writer = null;
        try {
            setAttachmentsMap(messageInfo, response);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            writer = XMLStreamWriterFactory.createXMLStreamWriter(baos);
            startEnvelope(writer);
            writeHeaders(writer, response, messageInfo);
            writeBody(writer, response, messageInfo);
            endEnvelope(writer);
            writer.flush();
            byte[] buf = baos.toByteArray();
            ByteInputStream bis = new ByteInputStream(buf, 0, buf.length);
            MimeHeaders mh = new MimeHeaders();
            mh.addHeader("Content-Type", getContentType(messageInfo));
            SOAPMessage msg = new SOAPMessageContext().createMessage(mh, bis);
            processAttachments(response, msg);
            return msg;
        } catch(Exception e) {
            throw new ServerRtException("soapencoder.err", new Object[]{e});
        } finally {
            if (writer != null) {
                try {
                    writer.writeEndDocument();                    
                    writer.close();
                }
                catch (XMLStreamException e) {
                    throw new ServerRtException(new LocalizableExceptionAdapter(e));            
                }
            }
        }
    }

    protected String getContentType(MessageInfo messageInfo){
        Object rtc = messageInfo.getMetaData(BindingProviderProperties.JAXWS_RUNTIME_CONTEXT);
        if(rtc != null){
            BridgeContext bc = ((RuntimeContext)rtc).getBridgeContext();
            if(bc != null){
                JAXWSAttachmentMarshaller am = (JAXWSAttachmentMarshaller)bc.getAttachmentMarshaller();
                if(am.isXopped())
                    return "application/xop+xml;type=\"text/xml\"";
                }
        }
        return "text/xml";
    }

    public ByteBuffer encode(MessageInfo messageInfo) {
        throw new UnsupportedOperationException();
    }

    /*
     * @see com.sun.pept.encoding.Encoder#encodeAndSend(com.sun.pept.ept.MessageInfo)
     */
    public void encodeAndSend(MessageInfo messageInfo) {
        throw new UnsupportedOperationException();
    }

    /*
     * writes <env:Fault> ... </env:Fault>. JAXB serializes the contents
     * in the <detail> for service specific exceptions. We serialize protocol
     * specific exceptions ourselves
     */
    protected void writeFault(SOAPFaultInfo instance, MessageInfo messageInfo, XMLStreamWriter writer) {
        try {
            writer.writeStartElement(SOAPNamespaceConstants.NSPREFIX_SOAP_ENVELOPE,
                SOAPConstants.QNAME_SOAP_FAULT.getLocalPart(),
                SOAPConstants.QNAME_SOAP_FAULT.getNamespaceURI());

            writer.writeStartElement(FAULTCODE_NAME);   // <faultcode>
            String prefix = SOAPNamespaceConstants.NSPREFIX_SOAP_ENVELOPE;
            QName faultCode = instance.getCode();
            String nsURI = faultCode.getNamespaceURI();
            if (!nsURI.equals(SOAPNamespaceConstants.ENVELOPE)) {
                    // Need to add namespace declaration for this custom fault code
                if (nsURI.equals(XMLConstants.NULL_NS_URI)) {
                    prefix = XMLConstants.DEFAULT_NS_PREFIX;
                } else {
                    prefix = faultCode.getPrefix();
                    if (prefix.equals(XMLConstants.DEFAULT_NS_PREFIX)) {
                        prefix = "ans";
                    }
                    writer.setPrefix(prefix, nsURI);
                    writer.writeNamespace(prefix, nsURI);
                }
            }
            if (prefix.equals(XMLConstants.DEFAULT_NS_PREFIX)) {
                writer.writeCharacters(instance.getCode().getLocalPart());
            } else {
                    writer.writeCharacters(prefix+":"+instance.getCode().getLocalPart());
            }
            writer.writeEndElement();                    // </faultcode>

            writer.writeStartElement(FAULTSTRING_NAME);
            writer.writeCharacters(instance.getString());
            writer.writeEndElement();

            if (instance.getActor() != null) {
                writer.writeStartElement(FAULTACTOR_NAME);
                writer.writeCharacters(instance.getActor());
                writer.writeEndElement();
            }

            Object detail = instance.getDetail();
            if (detail != null) {
                // Not RuntimeException, Not header fault
                if (detail instanceof Detail) {
                    // SOAPFaultException
                    encodeDetail((Detail)detail, writer);
                } else if (detail instanceof JAXBBridgeInfo) {
                    // Service specific exception
                    writer.writeStartElement(DETAIL_NAME);
                    writeJAXBBridgeInfo((JAXBBridgeInfo)detail, messageInfo, writer);
                    writer.writeEndElement();        // </detail>
                }
            }

            writer.writeEndElement();                // </env:Fault>
        }
        catch (XMLStreamException e) {
            throw new ServerRtException(new LocalizableExceptionAdapter(e));
        }
    }

    /*
     * Serializes javax.xml.soap.Detail. Detail is of type SOAPElement.
     * XmlTreeReader is used to traverse the SOAPElement/DOM Node and serializes
     * the XML.
     */
    protected void encodeDetail(Detail detail, XMLStreamWriter writer) {
        serializeReader(new DOMStreamReader(detail), writer);
    }

}
