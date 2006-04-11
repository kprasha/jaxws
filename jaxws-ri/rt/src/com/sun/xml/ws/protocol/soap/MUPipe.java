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
package com.sun.xml.ws.protocol.soap;

import static com.sun.xml.ws.api.SOAPVersion.SOAP_12;
import static com.sun.xml.ws.api.SOAPVersion.SOAP_11;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.message.Header;
import com.sun.xml.ws.api.message.HeaderList;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Messages;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.PipeCloner;
import com.sun.xml.ws.api.pipe.helper.AbstractFilterPipeImpl;
import com.sun.xml.ws.sandbox.message.impl.DOMHeader;

import javax.xml.namespace.QName;
import javax.xml.soap.*;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.soap.SOAPFaultException;
import javax.xml.ws.soap.SOAPBinding;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import org.w3c.dom.Element;

/**
 * @author Rama Pulavarthi
 */

public abstract class MUPipe extends AbstractFilterPipeImpl {

    private static final String MU_FAULT_DETAIL_LOCALPART = "NotUnderstood";
    private final static QName MU_HEADER_DETAIL = new QName(SOAPVersion.SOAP_12.nsUri, MU_FAULT_DETAIL_LOCALPART);
    //TODO: change
    protected static final Logger logger = Logger.getLogger(
            com.sun.xml.ws.util.Constants.LoggingDomain + ".soap.decoder");
    private final static String MUST_UNDERSTAND_FAULT_MESSAGE_STRING =
            "One or more mandatory SOAP header blocks not understood";

    private final SOAPVersion soapVersion;

    public MUPipe(WSBinding binding, Pipe next) {
        super(next);
        // MUPipe should n't be used for bindings other than SOAP.
        if (!(binding instanceof SOAPBinding)) {
            throw new WebServiceException(
                    "MUPipe should n't be used for bindings other than SOAP.");
        }
        this.soapVersion = binding.getSOAPVersion();


    }

    protected MUPipe(MUPipe that, PipeCloner cloner) {
        super(that, cloner);
        soapVersion = that.soapVersion;
    }

    /**
     * @param headers      HeaderList that needs MU processing
     * @param roles        Roles configured on the Binding. Required Roles supposed to be assumbed a by a
     *                     SOAP Binding implementation are added.
     * @param knownHeaders Set of headers that this binding understands
     * @return returns the headers that have mustUnderstand attribute and are not understood
     *         by the binding.
     */
    protected final Set<QName> getMisUnderstoodHeaders(HeaderList headers, Set<String> roles, Set<QName> knownHeaders) {
        //Add default roles assumed by SOAP Binding.
        //DONE in SOAPBindingImpl
        //roles.addAll(getRequiredRoles());

        Set<QName> notUnderstoodHeaders = null;

        for (int i = 0; i < headers.size(); i++) {
            if (!headers.isUnderstood(i)) {
                Header header = headers.get(i);
                if (!header.isIgnorable(soapVersion, roles)) {
                    QName qName = new QName(header.getNamespaceURI(), header.getLocalPart());
                    if (! knownHeaders.contains(qName)) {
                        logger.finest("Element not understood=" + qName);
                        if (notUnderstoodHeaders == null)
                            notUnderstoodHeaders = new HashSet<QName>();
                        notUnderstoodHeaders.add(qName);
                    }
                }
            }
        }
        return notUnderstoodHeaders;
    }

    /**
     * @param notUnderstoodHeaders
     * @return SOAPfaultException with SOAPFault representing the MustUnderstand SOAP Fault.
     *         notUnderstoodHeaders are added in the fault detail.
     */
    final SOAPFaultException createMUSOAPFaultException(Set<QName> notUnderstoodHeaders) {
        try {
            SOAPFault fault = createMUSOAPFault();
            addDetail(fault, notUnderstoodHeaders);
            return new SOAPFaultException(fault);
        } catch (SOAPException e) {
            throw new WebServiceException(e);
        }
    }

    /**
     * This should be used only in ServerMUPipe
     *
     * @param notUnderstoodHeaders
     * @return Message representing a SOAPFault
     *         In SOAP 1.1, notUnderstoodHeaders are added in the fault Detail
     *         in SOAP 1.2, notUnderstoodHeaders are added as the SOAP Headers
     */

    final Message createMUSOAPFaultMessage(Set<QName> notUnderstoodHeaders) {
        try {
            SOAPFault fault = createMUSOAPFault();
            if (soapVersion == SOAP_11) {
                addDetail(fault, notUnderstoodHeaders);
            }
            Message muFaultMessage = Messages.create(fault);
            if (soapVersion == SOAP_12) {
                addHeader(muFaultMessage, notUnderstoodHeaders);
            }
            return muFaultMessage;
        } catch (SOAPException e) {
            throw new WebServiceException(e);
        }
    }

    private static void addDetail(SOAPFault fault, Set<QName> notUnderstoodHeaders) throws SOAPException {
        Detail detail = fault.addDetail();
        for (QName qname : notUnderstoodHeaders) {
            //TODO change namespace
            Element e = fault.getOwnerDocument().createElementNS("http://TODO_RENAME", MU_FAULT_DETAIL_LOCALPART);
            e.setAttribute("xmlns:abc", qname.getNamespaceURI());
            e.setAttribute("qname", "abc:" + qname.getLocalPart());
            detail.appendChild(e);
        }

    }

    private static void addHeader(Message m, Set<QName> notUnderstoodHeaders) throws SOAPException {
        for (QName qname : notUnderstoodHeaders) {
            SOAPElement soapEl = SOAP_12.saajSoapFactory.createElement(MU_HEADER_DETAIL);
            soapEl.addNamespaceDeclaration("abc", qname.getNamespaceURI());
            soapEl.setAttribute("qname", "abc:" + qname.getLocalPart());
            Header header = new DOMHeader<Element>(soapEl);
            m.getHeaders().add(header);
        }
    }

    private SOAPFault createMUSOAPFault() throws SOAPException {
        return soapVersion.saajSoapFactory.createFault(
                MUST_UNDERSTAND_FAULT_MESSAGE_STRING,
                soapVersion.faultCodeMustUnderstand);
    }
}
