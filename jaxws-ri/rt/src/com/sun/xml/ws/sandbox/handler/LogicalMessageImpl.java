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

package com.sun.xml.ws.sandbox.handler;

import com.sun.xml.stream.buffer.XMLStreamBuffer;
import com.sun.xml.stream.buffer.XMLStreamBufferSource;
import com.sun.xml.stream.buffer.sax.SAXBufferCreator;
import com.sun.xml.stream.buffer.sax.SAXBufferProcessor;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.encoding.soap.DeserializationException;
import com.sun.xml.ws.encoding.soap.SerializationException;
import com.sun.xml.ws.util.ASCIIUtility;
import com.sun.xml.ws.util.xml.XmlUtil;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.xml.bind.JAXBContext;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.LogicalMessage;
import javax.xml.ws.WebServiceException;
import com.sun.xml.ws.encoding.jaxb.JAXBTypeSerializer;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Implementation of {@link LogicalMessage}. This class implements the methods
 * used by LogicalHandlers to get/set the request or response either
 * as a JAXB object or as javax.xml.transform.Source.
 *
 * <p>The {@link Message} that is passed into the constructor
 * is used to retrieve the payload of the request or response.
 *
 * @see Message
 * @see LogicalMessageContextImpl
 *
 * @author WS Development Team
 */
/**
* TODO: Take care of variations in behavior wrt to vaious sources.
* DOMSource : changes made should be reflected, StreamSource or SAXSource, Give copy
*/
public class LogicalMessageImpl implements LogicalMessage {
    private Packet packet;
    // This holds the (modified)payload set by User
    protected Source payloadSrc = null;
        
    /** Creates a new instance of LogicalMessageImplRearch */
    public LogicalMessageImpl(Packet packet) {
        // don't create extract payload until Users wants it.
        this.packet = packet;
    }
    
    public Source getPayload() {                
        
        if(payloadSrc == null) {
            payloadSrc = packet.getMessage().readPayloadAsSource();
        }
        if(payloadSrc instanceof DOMSource){
            return payloadSrc;
        } else {
            try {
            Transformer transformer = XmlUtil.newTransformer();
            DOMResult domResult = new DOMResult();
            transformer.transform(payloadSrc, domResult);
            payloadSrc = new DOMSource(domResult.getNode());
            return payloadSrc;
            } catch(TransformerException te) {
                throw new WebServiceException(te);
            }
        }
        /*
        Source copySrc;
        if(payloadSrc instanceof DOMSource){
            copySrc = payloadSrc;
        } else {
            copySrc = copy(payloadSrc);
        }
        return copySrc;
         */
    }
    
    public void setPayload(Source payload) {
        payloadSrc = payload;
    }
    
    public Object getPayload(JAXBContext context) {
        try {
            Source src = getPayload(); 
            return JAXBTypeSerializer.deserialize(src, context);   
        } catch (DeserializationException e){
            // As per Spec, try to give the original JAXBException
            throw new WebServiceException(e.getCause());
        }
    }
    
    public void setPayload(Object payload, JAXBContext context) {
        try {
            payloadSrc = JAXBTypeSerializer.serialize(payload, context);            
        } catch(SerializationException e) {
            // As per Spec, try to give the original JAXBException
            throw new WebServiceException(e.getCause());
        }        
    }
    /*
    private Source copy(Source src) {
        if(src instanceof StreamSource){
            StreamSource origSrc = (StreamSource)src;
            byte[] payloadbytes;
            try {
                payloadbytes = ASCIIUtility.getBytes(origSrc.getInputStream());
            } catch (IOException e) {
                throw new WebServiceException(e);
            }
            ByteArrayInputStream bis = new ByteArrayInputStream(payloadbytes);
            origSrc.setInputStream(new ByteArrayInputStream(payloadbytes));
            StreamSource copySource = new StreamSource(bis, src.getSystemId());
            return copySource;
        } else if(src instanceof SAXSource){
            SAXSource saxSrc = (SAXSource)src;
            try {
                XMLStreamBuffer xsb = new XMLStreamBuffer();
                XMLReader reader = saxSrc.getXMLReader();
                if(reader == null)
                    reader = new SAXBufferProcessor();
                saxSrc.setXMLReader(reader);
                reader.setContentHandler(new SAXBufferCreator(xsb));
                reader.parse(saxSrc.getInputSource());
                src = new XMLStreamBufferSource(xsb);
                return new XMLStreamBufferSource(xsb);
            } catch (IOException e) {
                throw new WebServiceException(e);
            } catch (SAXException e) {
                throw new WebServiceException(e);
            }
        }
        throw new WebServiceException("Copy is not needed for this Source");
    }
     */
}
