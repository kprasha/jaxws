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

import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.encoding.soap.DeserializationException;
import com.sun.xml.ws.encoding.soap.SerializationException;
import com.sun.xml.ws.handler.*;
import com.sun.xml.ws.sandbox.message.impl.source.PayloadSourceMessage;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.transform.Source;
import javax.xml.ws.LogicalMessage;
import javax.xml.ws.WebServiceException;
import com.sun.xml.ws.encoding.jaxb.JAXBTypeSerializer;

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
    private Message msg;    
    // This holds the (modified)payload set by User
    protected Source payloadSrc = null; 
    
    /** Creates a new instance of LogicalMessageImplRearch */
    public LogicalMessageImpl(Message msg) {
        // don't create extract payload until Users wants it.
        this.msg = msg;
    }
    
    public Source getPayload() {                
        if(payloadSrc == null) {
            payloadSrc = msg.readPayloadAsSource();            
        }
        return payloadSrc;
    }
    
    public void setPayload(Source payload) {
        payloadSrc = payload;
    }
    
    public Object getPayload(JAXBContext context) {
        try {
            if(payloadSrc == null) {
                payloadSrc = msg.readPayloadAsSource();                
            } 
            return JAXBTypeSerializer.deserialize(payloadSrc, context);   
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
}
