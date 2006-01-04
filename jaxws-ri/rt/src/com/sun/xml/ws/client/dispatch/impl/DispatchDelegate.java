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
package com.sun.xml.ws.client.dispatch.impl;

import com.sun.xml.ws.pept.ept.ContactInfo;
import com.sun.xml.ws.pept.ept.ContactInfoList;
import com.sun.xml.ws.pept.ept.ContactInfoListIterator;
import com.sun.xml.ws.pept.ept.MessageInfo;
import com.sun.xml.ws.pept.presentation.MessageStruct;
import com.sun.xml.ws.pept.protocol.MessageDispatcher;
import com.sun.xml.ws.encoding.soap.internal.DelegateBase;
import com.sun.xml.ws.encoding.soap.SOAPEncoder;
import com.sun.xml.ws.encoding.soap.client.SOAP12XMLEncoder;
import com.sun.xml.ws.client.*;
import com.sun.xml.ws.client.dispatch.DispatchContext;

import javax.xml.ws.BindingProvider;
import static javax.xml.ws.BindingProvider.*;

import static com.sun.xml.ws.client.BindingProviderProperties.*;

import javax.xml.ws.WebServiceException;
import javax.xml.ws.Service;
import javax.xml.ws.soap.SOAPBinding;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.MimeHeader;
import javax.xml.soap.SOAPException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.*;

import com.sun.xml.ws.binding.BindingImpl;
import com.sun.xml.ws.sandbox.message.Message;
import com.sun.xml.ws.sandbox.message.MessageProperties;
import com.sun.xml.ws.sandbox.message.impl.saaj.SAAJMessage;
import com.sun.xml.ws.sandbox.impl.TestEncoderImpl;
import com.sun.xml.ws.sandbox.impl.TestDecoderImpl;
import com.sun.xml.ws.transport.http.client.HttpTransportPipe;
import com.sun.xml.ws.spi.runtime.ClientTransportFactory;
import com.sun.xml.ws.spi.runtime.MessageContext;
import com.sun.xml.ws.util.Base64Util;
import com.sun.xml.ws.server.RuntimeContext;
import com.sun.xml.ws.model.JavaMethod;
import com.sun.xml.messaging.saaj.soap.MessageImpl;

/**
 * @author WS Development Team
 */
public class DispatchDelegate extends DelegateBase {

    private static final Logger logger =
        Logger.getLogger(new StringBuffer().append(com.sun.xml.ws.util.Constants.LoggingDomain).append(".client.dispatch").toString());

    public DispatchDelegate() {
    }

    public DispatchDelegate(ContactInfoList contactInfoList) {
        this.contactInfoList = contactInfoList;
    }

    public void send(MessageStruct messageStruct) {
        Message msg = null;

        MessageInfo messageInfo = (MessageInfo) messageStruct;

        ContextMap properties = (ContextMap)
            messageInfo.getMetaData(BindingProviderProperties.JAXWS_CONTEXT_PROPERTY);
        BindingProvider dispatch = (BindingProvider) properties.get(BindingProviderProperties.JAXWS_CLIENT_HANDLE_PROPERTY);

        //if (!contactInfoList.iterator().hasNext())
        //    throw new WebServiceException("can't pickup message encoder/decoder, no ContactInfo!");

        if (messageInfo.getMetaData(DispatchContext.DISPATCH_MESSAGE_MODE) == Service.Mode.MESSAGE)
            msg = (Message) messageInfo.getData()[0];

        //temp
        MessageProperties context = processMetadata(messageInfo, msg);


        if (!isAsync(messageInfo)) {

            HttpTransportPipe dispatcher =
                new HttpTransportPipe(TestEncoderImpl.INSTANCE, TestDecoderImpl.INSTANCE11, context);

            Message response = dispatcher.process(msg);
            SOAPMessage result = null;
            try {
                result = response.readAsSOAPMessage();
            } catch (SOAPException e) {
                e.printStackTrace();
            }
            messageInfo.setResponse(result);

        }

    }

    /*private ContactInfo getContactInfo(ContactInfoList cil, String bindingId){
         ContactInfoListIterator iter = cil.iterator();
         while(iter.hasNext()){
             ContactInfoBase cib = (ContactInfoBase)iter.next();
             if(cib.getBindingId().equals(bindingId))
                 return cib;
         }
         //return the first one
         return cil.iterator().next();
     }
    */

    //temp-kw
    protected MessageProperties processMetadata(MessageInfo messageInfo, Message saajMessage) {
        // Map<String, Object> messageContext = new HashMap<String, Object>();
        MessageProperties messageProps = saajMessage.getProperties();
        Map<String, List<String>> requestHeaders = messageProps.httpRequestHeaders;
        if (requestHeaders == null) {
            requestHeaders = new HashMap<String, List<String>>();
            messageProps.httpRequestHeaders = requestHeaders;
        }

        //just make sure this is getting set for now-temp
        ArrayList ct = new ArrayList<String>();
        ct.add("text/xml");
        requestHeaders.put("Content-Type", ct);
        ArrayList cte = new ArrayList<String>();
        cte.add("binary");
        requestHeaders.put("Content-Transfer-Encoding", cte);
        ContextMap properties = (ContextMap) messageInfo.getMetaData(JAXWS_CONTEXT_PROPERTY);

        if (messageInfo.getMEP().isOneWay())
            messageProps.put(ONE_WAY_OPERATION, "true");

        String soapAction = null;
        boolean useSoapAction = false;

        // process the properties
        if (properties != null) {
            for (Iterator names = properties.getPropertyNames(); names.hasNext();)
            {
                String propName = (String) names.next();

                // consume PEPT-specific properties
                if (propName.equals(ClientTransportFactory.class.getName())) {
                    messageProps.put(CLIENT_TRANSPORT_FACTORY, (ClientTransportFactory) properties.get(propName));
                } else if (propName.equals(USERNAME_PROPERTY)) {
                    String credentials = (String) properties.get(USERNAME_PROPERTY);
                    if (credentials != null) {
                        credentials += ":";
                        String password = (String) properties.get(PASSWORD_PROPERTY);
                        if (password != null)
                            credentials += password;

                        try {
                            credentials = Base64Util.encode(credentials.getBytes());
                        } catch (Exception ex) {
                            throw new WebServiceException(ex);
                        }
                        //kwsoapMessage.getMimeHeaders().addHeader("Authorization", "Basic " + credentials);
                    }
                } else
                if (propName.equals(BindingProvider.SOAPACTION_USE_PROPERTY)) {
                    useSoapAction = ((Boolean)
                        properties.get(BindingProvider.SOAPACTION_USE_PROPERTY)).booleanValue();
                    if (useSoapAction)
                        soapAction = (String)
                            properties.get(BindingProvider.SOAPACTION_URI_PROPERTY);
                } else {
                    messageProps.put(propName, properties.get(propName));
                }
            }
        }

        // Set accept header depending on content negotiation property
        String contentNegotiation = (String) messageInfo.getMetaData(CONTENT_NEGOTIATION_PROPERTY);

        String bindingId = getBindingId(messageInfo);
        ArrayList cn = new ArrayList<String>();
        if (bindingId.equals(SOAPBinding.SOAP12HTTP_BINDING)) {

            //soapMessage.getMimeHeaders().setHeader(ACCEPT_PROPERTY,
            //    contentNegotiation != "none" ? SOAP12_XML_FI_ACCEPT_VALUE : SOAP12_XML_ACCEPT_VALUE);
        } else {
            cn.add(XML_ACCEPT_VALUE);
            requestHeaders.put(ACCEPT_PROPERTY, cn);
            // soapMessage.getMimeHeaders().setHeader(ACCEPT_PROPERTY,
            //     contentNegotiation != "none" ? XML_FI_ACCEPT_VALUE : XML_ACCEPT_VALUE);
        }

        messageProps.put(BINDING_ID_PROPERTY, bindingId);

        // SOAPAction: MIME header
        RuntimeContext runtimeContext = (RuntimeContext) messageInfo.getMetaData(JAXWS_RUNTIME_CONTEXT);
        if (runtimeContext != null) {
            JavaMethod javaMethod = runtimeContext.getModel().getJavaMethod(messageInfo.getMethod());
            if (javaMethod != null) {
                soapAction = ((com.sun.xml.ws.model.soap.SOAPBinding) javaMethod.getBinding()).getSOAPAction();
                //header.clear();
                if (bindingId.equals(SOAPBinding.SOAP12HTTP_BINDING)) {
                    if ((soapAction != null) && (soapAction.length() > 0)) {
                        //((MessageImpl) soapMessage).setAction(soapAction);
                    }
                } else {
                    if (soapAction == null) {
                        //soapMessage.getMimeHeaders().addHeader("SOAPAction", "\"\"");
                    } else {
                        //soapMessage.getMimeHeaders().addHeader("SOAPAction", "\"" + soapAction + "\"");
                    }
                }
            }
        } else
        if (messageInfo.getMetaData(BindingProviderProperties.DISPATCH_CONTEXT) != null)
        {
            //bug fix 6344358
            //header.clear();
            if (soapAction == null) {
                //soapMessage.getMimeHeaders().addHeader("SOAPAction", "\"\"");
            } else {
                //soapMessage.getMimeHeaders().addHeader("SOAPAction", "\"" + soapAction + "\"");
            }
        }

        return messageProps;
    }


    /**
     * @return true if message exchange pattern indicates asynchronous, otherwise returns false
     */
    protected boolean isAsync(MessageInfo messageInfo) {
        return messageInfo.getMEP().isAsync;
    }

    private void setDefaultEncoding(RequestContext requestContext) {
        requestContext.put(BindingProviderProperties.ACCEPT_ENCODING_PROPERTY,
            BindingProviderProperties.XML_ENCODING_VALUE);
    }

    /**
     * This method is used to create the appropriate SOAPMessage (1.1 or 1.2 using SAAJ api).
     *
     * @return the BindingId associated with messageInfo
     */
    protected String getBindingId(MessageInfo messageInfo) {
        SOAPEncoder encoder = (SOAPEncoder) messageInfo.getEncoder();
        if (encoder instanceof SOAP12XMLEncoder)
            return SOAPBinding.SOAP12HTTP_BINDING;
        else
            return SOAPBinding.SOAP11HTTP_BINDING;
    }


}
