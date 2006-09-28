/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License).  You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the license at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * 
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.xml.ws.api.addressing;

import com.sun.xml.stream.buffer.XMLStreamBuffer;
import com.sun.xml.ws.addressing.W3CAddressingConstants;
import com.sun.xml.ws.addressing.WsaPipeHelper;
import com.sun.xml.ws.addressing.v200408.MemberSubmissionAddressingConstants;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.message.Header;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.message.stream.OutboundStreamHeader;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.ws.soap.AddressingFeature;

/**
 * 'Traits' object that absorbs differences of WS-Addressing versions.
 *
 * @author Arun Gupta
 */
public enum AddressingVersion {
    W3C(W3CAddressingConstants.WSA_NAMESPACE_NAME,"w3c-anonymous-epr.xml") {
        @Override
        public boolean isReferenceParameter(String localName) {
            return localName.equals("ReferenceParameters");
        }

        @Override
        public WsaPipeHelper getWsaHelper(WSDLPort wsdlPort, WSBinding binding) {
            return new com.sun.xml.ws.addressing.WsaPipeHelperImpl(wsdlPort, binding);
        }

        @Override
        public String getNoneUri() {
            return nsUri + "/none";
        }

        @Override
        public String getAnonymousUri() {
            return nsUri + "/anonymous";
        }

        @Override
        public String getMapRequiredLocalName() {
            return "MessageAddressingHeaderRequired";
        }

        @Override
        public String getMapRequiredText() {
            return "A required header representing a Message Addressing Property is not present";
        }

        @Override
        public String getInvalidMapLocalName() {
            return "InvalidAddressingHeader";
        }

        @Override
        public String getInvalidMapText() {
            return "A header representing a Message Addressing Property is not valid and the message cannot be processed";
        }

        @Override
        public String getInvalidCardinalityLocalName() {
            return "InvalidCardinality";
        }

        /*package*/ Header createReferenceParameterHeader(XMLStreamBuffer mark, String nsUri, String localName) {
            return new OutboundReferenceParameterHeader(mark,nsUri,localName);
        }
    },
    MEMBER(MemberSubmissionAddressingConstants.WSA_NAMESPACE_NAME,"member-anonymous-epr.xml") {
        @Override
        public boolean isReferenceParameter(String localName) {
            return localName.equals("ReferenceParameters") || localName.equals("ReferenceProperties");
        }

        @Override
        public WsaPipeHelper getWsaHelper(WSDLPort wsdlPort, WSBinding binding) {
            return new com.sun.xml.ws.addressing.v200408.WsaPipeHelperImpl(wsdlPort, binding);
        }

        @Override
        public String getNoneUri() {
            return "";
        }

        @Override
        public String getAnonymousUri() {
            return nsUri + "/role/anonymous";
        }

        @Override
        public String getMapRequiredLocalName() {
            return "MessageInformationHeaderRequired";
        }

        @Override
        public String getMapRequiredText() {
            return "A required message information header, To, MessageID, or Action, is not present.";
        }

        @Override
        public String getInvalidMapLocalName() {
            return "InvalidMessageInformationHeader";
        }

        @Override
        public String getInvalidMapText() {
            return "A message information header is not valid and the message cannot be processed.";
        }

        @Override
        public String getInvalidCardinalityLocalName() {
            return getInvalidMapLocalName();
        }

        /*package*/ Header createReferenceParameterHeader(XMLStreamBuffer mark, String nsUri, String localName) {
            return new OutboundStreamHeader(mark,nsUri,localName);
        }
    };

    public final String nsUri;

    /**
     * Represents the anonymous EPR.
     */
    public final WSEndpointReference anonymousEpr;
    /**
     * Represents the ReplyTo for a specific WS-Addressing Version.
     * For example, wsa:ReplyTo where wsa binds to "http://www.w3.org/2005/08/addressing"
     */
    public final QName toTag;
    public final QName fromTag;
    public final QName replyToTag;
    public final QName faultToTag;
    public final QName actionTag;
    public final QName messageIDTag;
    public final QName relatesToTag;

    public final QName mapRequiredTag;
    public final QName actionNotSupportedTag;
    public final String actionNotSupportedText;
    public final QName invalidMapTag;
    public final QName invalidCardinalityTag;
    public final QName problemHeaderQNameTag;

    /**
     * Fault sub-sub-code that represents
     * "Specifies that the invalid header was expected to be an EPR but did not contain an [address]."
     */
    public final QName fault_missingAddressInEpr;

    private static final String EXTENDED_FAULT_NAMESPACE = "http://jax-ws.dev.java.net/addressing/fault";

    /**
     * Fault sub-sub-code that represents duplicate &lt;Address> element in EPR.
     * This is a fault code not defined in the spec.
     */
    public static final QName fault_duplicateAddressInEpr = new QName(
        EXTENDED_FAULT_NAMESPACE, "DuplicateAddressInEpr"
    );



    private AddressingVersion(String nsUri, String anonymousEprResourceName) {
        this.nsUri = nsUri;
        toTag = new QName(nsUri,"To");
        fromTag = new QName(nsUri,"From");
        replyToTag = new QName(nsUri,"ReplyTo");
        faultToTag = new QName(nsUri,"FaultTo");
        actionTag = new QName(nsUri,"Action");
        messageIDTag = new QName(nsUri,"MessageID");
        relatesToTag = new QName(nsUri,"RelatesTo");

        mapRequiredTag = new QName(nsUri,getMapRequiredLocalName());
        actionNotSupportedTag = new QName(nsUri,"ActionNotSupported");
        actionNotSupportedText = "The \"%s\" cannot be processed at the receiver";
        invalidMapTag = new QName(nsUri,getInvalidMapLocalName());
        invalidCardinalityTag = new QName(nsUri,getInvalidCardinalityLocalName());

        problemHeaderQNameTag = new QName(nsUri,"ProblemHeaderQName");

        fault_missingAddressInEpr = new QName(nsUri,"MissingAddressInEPR","wsa");

        // create stock anonymous EPR
        try {
            this.anonymousEpr = new WSEndpointReference(getClass().getResourceAsStream(anonymousEprResourceName),this);
        } catch (XMLStreamException e) {
            throw new Error(e); // bug in our code as EPR should parse.
        }
    }

    /**
     * Returns {@link AddressingVersion} whose {@link #nsUri} equals to
     * the given string.
     *
     * This method does not perform input string validation.
     *
     * @param nsUri
     *      must not be null.
     * @return always non-null.
     */
    public static AddressingVersion fromNsUri(String nsUri) {
        if (nsUri.equals(W3C.nsUri))
            return W3C;

        if (nsUri.equals(MEMBER.nsUri))
            return MEMBER;

        return null;
    }

    public static AddressingVersion fromBinding(WSBinding binding) {
        if (binding.hasFeature(AddressingFeature.ID))
            return W3C;

        if (binding.hasFeature(MemberSubmissionAddressingFeature.ID))
            return MEMBER;

        return null;
    }

    public static AddressingVersion fromPort(WSDLPort port) {
        String ns = port.getBinding().getAddressingVersion();
        if (ns.equals(W3C.nsUri))
            return W3C;

        if (ns.equals(MEMBER.nsUri))
            return MEMBER;

        return null;
    }

    /**
     * Returns {@link #nsUri} associated with this {@link AddressingVersion}
     *
     * @return namespace URI
     */
    public String getNsUri() {
        return nsUri;
    }

    /**
     * Returns true if the given local name is considered as
     * a reference parameter in EPR.
     *
     * For W3C, this means "ReferenceParameters",
     * and for the member submission version, this means
     * either "ReferenceParameters" or "ReferenceProperties".
     */
    public abstract boolean isReferenceParameter(String localName);

    public abstract WsaPipeHelper getWsaHelper(WSDLPort wsdlPort, WSBinding binding);

    public abstract String getNoneUri();

    public abstract String getAnonymousUri();

    public String getDefaultFaultAction() {
        return nsUri + "/fault";
    }

    public QName getFaultDetailQName() {
        return new QName(nsUri, "FaultDetail");
    }

    protected abstract String getMapRequiredLocalName();

    public abstract String getMapRequiredText();

    public abstract String getInvalidMapLocalName();

    public abstract String getInvalidMapText();

    public abstract String getInvalidCardinalityLocalName();

    /**
     * Creates an outbound {@link Header} from a referene parameter.
     */
    /*package*/ abstract Header createReferenceParameterHeader(XMLStreamBuffer mark, String nsUri, String localName);

//
//    private SOAPFault newActionNotSupportedFault(String action, WSBinding binding) {
//        QName subcode = actionNotSupportedTag;
//        String faultstring = String.format(actionNotSupportedText, action);
//
//        try {
//            SOAPFactory factory;
//            SOAPFault fault;
//            if (binding.getSOAPVersion() == SOAPVersion.SOAP_12) {
//                factory = SOAPVersion.SOAP_12.saajSoapFactory;
//                fault = factory.createFault();
//                fault.setFaultCode(SOAPConstants.SOAP_SENDER_FAULT);
//                fault.appendFaultSubcode(subcode);
//                getProblemActionDetail(action, fault.addDetail());
//            } else {
//                factory = SOAPVersion.SOAP_11.saajSoapFactory;
//                fault = factory.createFault();
//                fault.setFaultCode(subcode);
//            }
//
//            fault.setFaultString(faultstring);
//
//            return fault;
//        } catch (SOAPException e) {
//            throw new WebServiceException(e);
//        }
//    }
}
