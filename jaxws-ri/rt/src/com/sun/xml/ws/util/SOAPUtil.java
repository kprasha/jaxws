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

package com.sun.xml.ws.util;

import com.sun.xml.ws.encoding.soap.SOAPVersion;
import com.sun.xml.ws.encoding.soap.message.SOAPMsgCreateException;
import com.sun.xml.ws.encoding.soap.message.SOAPMsgFactoryCreateException;
import org.w3c.dom.Node;

import javax.xml.namespace.QName;
import javax.xml.soap.Detail;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFactory;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.soap.SOAPBinding;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.net.URL;

/**
 * Has utility methods to create SOAPMessage
 *
 * <p>
 * This code is not nicely designed. It's not OO, and too error prone.
 * One of the good programming principles is to prefer a typed object over
 * its name. That means use {@link File} instead of a string that represents
 * a file name, use {@link URL} instead of a string that represents a URL.
 *
 * <p>
 * By the same token, you should use {@link SOAPVersion} instead of a string
 * that represents a SOAP version. Using a good type makes the code easier
 * to read, and also allow convenience methods to be defined on it (instead
 * of defining utility methods externally, which is so C!)
 *
 * $author: JAXWS Development Team
 *
 * @deprecated See the above justification.
 */
public class SOAPUtil {

    /**
     *
     * @param bindingId
     * @return
     *          returns SOAPFactor for SOAP 1.2 if bindingID equals SOAP1.2 HTTP binding else
     *          SOAPFactory for SOAP 1.1
     */
    public static SOAPFactory getSOAPFactory(String bindingId){
        return SOAPVersion.fromBinding(bindingId).saajSoapFactory;
    }

    public static SOAPFault createSOAPFault(String bindingId){
         try {
             return getSOAPFactory(bindingId).createFault();
        } catch (SOAPException e) {
            throw new SOAPMsgFactoryCreateException(
                "soap.fault.create.err",
                new Object[] { e });
        }
    }

    /**
     * Creates SOAP 1.1 or SOAP 1.2 SOAPFault based on the bindingId
     * @param msg
     * @param code
     * @param actor
     * @param detail
     * @return the created SOAPFault
     */
    public static SOAPFault createSOAPFault(String msg, QName code, String actor, Detail detail, String bindingId){
        try {
            SOAPFault fault = getSOAPFactory(bindingId).createFault(msg,code);

            if(actor != null)
                fault.setFaultActor(actor);
            if(detail != null){
                Node n = fault.getOwnerDocument().importNode(detail, true);
                fault.appendChild(n);
            }
            return fault;
        } catch (SOAPException e) {
            throw new SOAPMsgFactoryCreateException(
                "soap.fault.create.err",
                new Object[] { e });
        }
    }

    public static SOAPMessage createMessage() {
        return createMessage(SOAPBinding.SOAP11HTTP_BINDING);
    }

    /**
     *
     * @param binding
     * @return a <code>SOAPMessage</code> associated with <code>binding</code>
     */
    public static SOAPMessage createMessage(String binding) {
        try {
            return getMessageFactory(binding).createMessage();
        } catch (SOAPException e) {
            throw new SOAPMsgCreateException(
                    "soap.msg.create.err",
                    new Object[] { e });
        }
    }

    /**
     *
     * @param binding
     * @param headers
     * @param in
     * @return <code>SOAPMessage</code> with <code>MimeHeaders</code> from an
     *         <code>InputStream</code> and binding.
     * @throws IOException
     */
    public static SOAPMessage createMessage(MimeHeaders headers, InputStream in,
                                            String binding) throws IOException {
        try {
            return getMessageFactory(binding).createMessage(headers, in);
        } catch (SOAPException e) {
            throw new SOAPMsgCreateException(
                    "soap.msg.create.err",
                    new Object[] { e });
        }
    }

    public static SOAPMessage createMessage(MimeHeaders headers, InputStream in)
    throws IOException {
        return createMessage(headers, in, SOAPBinding.SOAP11HTTP_BINDING);
    }

    public static MessageFactory getMessageFactory(String binding) {
        return SOAPVersion.fromBinding(binding).saajFactory;
    }
}
