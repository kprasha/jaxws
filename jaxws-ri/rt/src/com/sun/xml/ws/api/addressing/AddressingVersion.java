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
        /* package */ String getMapRequiredLocalName() {
            return "MessageAddressingHeaderRequired";
        }

        @Override
        public String getMapRequiredText() {
            return "A required header representing a Message Addressing Property is not present";
        }

        @Override
        /* package */ String getInvalidMapLocalName() {
            return "InvalidAddressingHeader";
        }

        @Override
        public String getInvalidMapText() {
            return "A header representing a Message Addressing Property is not valid and the message cannot be processed";
        }

        @Override
        /* package */ String getInvalidCardinalityLocalName() {
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
        /* package */ String getMapRequiredLocalName() {
            return "MessageInformationHeaderRequired";
        }

        @Override
        public String getMapRequiredText() {
            return "A required message information header, To, MessageID, or Action, is not present.";
        }

        @Override
        /* package */ String getInvalidMapLocalName() {
            return "InvalidMessageInformationHeader";
        }

        @Override
        public String getInvalidMapText() {
            return "A message information header is not valid and the message cannot be processed.";
        }

        @Override
        /* package */ String getInvalidCardinalityLocalName() {
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
     * Represents the To QName for a specific WS-Addressing Version.
     */
    public final QName toTag;

    /**
     * Represents the From QName for a specific WS-Addressing Version.
     */
    public final QName fromTag;

    /**
     * Represents the ReplyTo QName for a specific WS-Addressing Version.
     */
    public final QName replyToTag;

    /**
     * Represents the FaultTo QName for a specific WS-Addressing Version.
     */
    public final QName faultToTag;

    /**
     * Represents the Action QName for a specific WS-Addressing Version.
     */
    public final QName actionTag;

    /**
     * Represents the MessageID QName for a specific WS-Addressing Version.
     */
    public final QName messageIDTag;

    /**
     * Represents the RelatesTo QName for a specific WS-Addressing Version.
     */
    public final QName relatesToTag;

    /**
     * Represents the QName of the fault code when a required header representing a
     * WS-Addressing Message Addressing Property is not present.
     */
    public final QName mapRequiredTag;
    public final QName actionNotSupportedTag;
    public final String actionNotSupportedText;

    /**
     * Represents the QName of the fault code when a header representing a
     * WS-Addressing Message Addressing Property is invalid and cannot be processed.
     */
    public final QName invalidMapTag;

    /**
     * Represents the QName of the fault code when a header representing a
     * WS-Addressing Message Addressing Property occurs greater than expected number.
     */
    public final QName invalidCardinalityTag;
    public final QName problemHeaderQNameTag;

    /**
     * Represents the QName of the header element that is used to capture the fault detail
     * if there is a fault processing WS-Addressing Message Addressing Property. This is
     * only used for SOAP 1.1.
     */
    public final QName faultDetailTag;

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
        faultDetailTag = new QName(nsUri,"FaultDetail");

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

    /**
     * Gets the {@link AddressingVersion} from a {@link WSBinding}
     *
     * @param binding WSDL binding
     * @return addresing version
     */
    public static final AddressingVersion fromBinding(WSBinding binding) {
        if (binding.hasFeature(AddressingFeature.ID))
            return W3C;

        if (binding.hasFeature(MemberSubmissionAddressingFeature.ID))
            return MEMBER;

        return null;
    }

    /**
     * Gets the {@link AddressingVersion} from a {@link WSDLPort}
     *
     * @param port WSDL port
     * @return addresing version
     */
    public static final AddressingVersion fromPort(WSDLPort port) {
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

    /**
     * Gets the none URI value associated with this WS-Addressing version.
     *
     * @return none URI value
     */
    public abstract String getNoneUri();

    /**
     * Gets the anonymous URI value associated with this WS-Addressing version.
     *
     * @return anonymous URI value
     */
    public abstract String getAnonymousUri();

    /**
     * Gets the default fault Action value associated with this WS-Addressing version.
     *
     * @return default fault Action value
     */
    public String getDefaultFaultAction() {
        return nsUri + "/fault";
    }

    /**
     * Gets the local name of the fault when a header representing a WS-Addressing Message
     * Addresing Property is absent.
     *
     * @return local name
     */
    /* package */ abstract String getMapRequiredLocalName();

    /**
     * Gets the description text when a required WS-Addressing header representing a
     * Message Addressing Property is absent.
     *
     * @return description text
     */
    public abstract String getMapRequiredText();

    /**
     * Gets the local name of the fault when a header representing a WS-Addressing Message
     * Addresing Property is invalid and cannot be processed.
     *
     * @return local name
     */
    /* package */ abstract String getInvalidMapLocalName();

    /**
     * Gets the description text when a header representing a WS-Addressing
     * Message Addressing Property is invalid and cannot be processed.
     *
     * @return description text
     */
    public abstract String getInvalidMapText();

    /**
     * Gets the local name of the fault when a header representing a WS-Addressing Message
     * Addresing Property occurs greater than expected number.
     *
     * @return local name
     */
    /* package */ abstract String getInvalidCardinalityLocalName();

    /**
     * Creates an outbound {@link Header} from a reference parameter.
     */
    /*package*/ abstract Header createReferenceParameterHeader(XMLStreamBuffer mark, String nsUri, String localName);
}
