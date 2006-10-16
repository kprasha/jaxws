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

import com.sun.istack.NotNull;
import com.sun.xml.stream.buffer.XMLStreamBuffer;
import com.sun.xml.ws.addressing.WsaPipeHelper;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.message.Header;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.message.stream.OutboundStreamHeader;
import com.sun.xml.ws.developer.MemberSubmissionAddressingFeature;
import com.sun.xml.ws.developer.MemberSubmissionEndpointReference;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.ws.EndpointReference;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.soap.AddressingFeature;
import javax.xml.ws.wsaddressing.W3CEndpointReference;

/**
 * 'Traits' object that absorbs differences of WS-Addressing versions.
 *
 * @author Arun Gupta
 */
public enum AddressingVersion {
    W3C("http://www.w3.org/2005/08/addressing",
        "w3c-anonymous-epr.xml",
        "http://www.w3.org/2006/05/addressing/wsdl",
        "http://www.w3.org/2006/05/addressing/wsdl",
        W3CEndpointReference.class) {
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

        /*package*/ String getIsReferenceParameterLocalName() {
            return "IsReferenceParameter";
        }

        /* package */ String getWsdlAnonymousLocalName() {
            return "Anonymous";
        }

        public String getPrefix() {
            return "wsa";
        }

        public String getWsdlPrefix() {
            return "wsaw";
        }

        public String getFeatureID() {
            return AddressingFeature.ID;
        }
    },
    MEMBER("http://schemas.xmlsoap.org/ws/2004/08/addressing",
           "member-anonymous-epr.xml",
           "http://schemas.xmlsoap.org/ws/2004/08/addressing",
           "http://schemas.xmlsoap.org/ws/2004/08/addressing/policy",
            MemberSubmissionEndpointReference.class) {
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

        /*package*/ String getIsReferenceParameterLocalName() {
            return "";
        }

        /* package */ String getWsdlAnonymousLocalName() {
            return "";
        }

        public String getPrefix() {
            return "wsa";
        }

        public String getWsdlPrefix() {
            return "wsaw";
        }

        public String getFeatureID() {
            return MemberSubmissionAddressingFeature.ID;
        }
    };

    /**
     * Namespace URI
     */
    public final String nsUri;

    /**
     * Namespace URI for the WSDL Binding
     */
    public final String wsdlNsUri;

    /**
     * Either {@link W3CEndpointReference} or {@link MemberSubmissionEndpointReference}.
     */
    public final Class<? extends EndpointReference> eprClass;

    /**
     * Namespace URI for the WSDL Binding
     */
    public final String policyNsUri;

    /**
     * Represents the anonymous EPR.
     */
    public final WSEndpointReference anonymousEpr;

    /**
     * Represents the To QName in the SOAP message for a specific WS-Addressing Version.
     */
    public final QName toTag;

    /**
     * Represents the From QName in the SOAP message for a specific WS-Addressing Version.
     */
    public final QName fromTag;

    /**
     * Represents the ReplyTo QName in the SOAP message for a specific WS-Addressing Version.
     */
    public final QName replyToTag;

    /**
     * Represents the FaultTo QName for a specific WS-Addressing Version.
     */
    public final QName faultToTag;

    /**
     * Represents the Action QName in the SOAP message for a specific WS-Addressing Version.
     */
    public final QName actionTag;

    /**
     * Represents the MessageID QName in the SOAP message for a specific WS-Addressing Version.
     */
    public final QName messageIDTag;

    /**
     * Represents the RelatesTo QName in the SOAP message for a specific WS-Addressing Version.
     */
    public final QName relatesToTag;

    /**
     * Represents the QName of the fault code when a required header representing a
     * WS-Addressing Message Addressing Property is not present.
     */
    public final QName mapRequiredTag;

    /**
     * Represents the QName of the fault code when Action is not supported at this endpoint.
     */
    public final QName actionNotSupportedTag;

    /**
     * Represents the text of the fault when Action is not supported at this endpoint.
     */
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

    /**
     * Represents the QName of the element that conveys additional information
     * on the pre-defined WS-Addressing faults.
     */
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

    /**
     * Represents the Action QName in the WSDL for a specific WS-Addressing Version.
     */
    public final QName wsdlActionTag;

    /**
     * Represents the WSDL extension QName for a specific WS-Addressing Version.
     */
    public final QName wsdlExtensionTag;

    /**
     * Represents the WSDL anonymous QName for a specific WS-Addressing Version.
     */
    public final QName wsdlAnonymousTag;

    /**
     * Represents the QName of the reference parameter in a SOAP message. This is
     * only valid for W3C WS-Addressing.
     */
    public final QName isReferenceParameterTag;

    private static final String EXTENDED_FAULT_NAMESPACE = "http://jax-ws.dev.java.net/addressing/fault";
    public static final String UNSET_OUTPUT_ACTION = "http://jax-ws.dev.java.net/addressing/output-action-not-set";
    public static final String UNSET_INPUT_ACTION = "http://jax-ws.dev.java.net/addressing/input-action-not-set";

    /**
     * Fault sub-sub-code that represents duplicate &lt;Address> element in EPR.
     * This is a fault code not defined in the spec.
     */
    public static final QName fault_duplicateAddressInEpr = new QName(
        EXTENDED_FAULT_NAMESPACE, "DuplicateAddressInEpr"
    );

    private AddressingVersion(String nsUri, String anonymousEprResourceName, String wsdlNsUri, String policyNsUri, Class<? extends EndpointReference> eprClass ) {
        this.nsUri = nsUri;
        this.wsdlNsUri = wsdlNsUri;
        this.policyNsUri = policyNsUri;
        this.eprClass = eprClass;
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
        isReferenceParameterTag = new QName(nsUri,getIsReferenceParameterLocalName());

        wsdlActionTag = new QName(wsdlNsUri,"Action");
        wsdlExtensionTag = new QName(wsdlNsUri, "UsingAddressing");
        wsdlAnonymousTag = new QName(wsdlNsUri, getWsdlAnonymousLocalName());

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
    public static AddressingVersion fromBinding(WSBinding binding) {
        if (binding.isFeatureEnabled(AddressingFeature.ID))
            return W3C;

        if (binding.isFeatureEnabled(MemberSubmissionAddressingFeature.ID))
            return MEMBER;

        return null;
    }

    /**
     * Gets the {@link AddressingVersion} from a {@link WSDLPort}
     *
     * @param port WSDL port
     * @return addresing version
     */
    public static AddressingVersion fromPort(WSDLPort port) {
        if (port == null)
            return null;
        
        WebServiceFeature wsf = port.getFeature(AddressingFeature.ID);
        if (wsf == null) {
            wsf = port.getFeature(MemberSubmissionAddressingFeature.ID);
        }
        if (wsf == null)
            return null;

        return fromFeature(wsf);
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

    /**
     * Returns WsaPipeHelper for the WS-Addressing version identified by <code>binding</code>
     * {@link WSBinding} and for the {@link WSDLPort} port.
     *
     * @return WS-A version specific helper
     */
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

    /* package */ abstract String getWsdlAnonymousLocalName();

    public abstract String getPrefix();

    public abstract String getWsdlPrefix();

    public abstract String getFeatureID();

    /**
     * Creates an outbound {@link Header} from a reference parameter.
     */
    /*package*/ abstract Header createReferenceParameterHeader(XMLStreamBuffer mark, String nsUri, String localName);

    /**
     * Gets the local name for wsa:IsReferenceParameter. This method will return a valid
     * value only valid for W3C WS-Addressing. For Member Submission WS-Addressing, this method
     * returns null.
     */
    /*package*/ abstract String getIsReferenceParameterLocalName();

    public static AddressingVersion fromFeature(WebServiceFeature af) {
        if (af.getID().equals(AddressingFeature.ID))
            return W3C;
        else if (af.getID().equals(MemberSubmissionAddressingFeature.ID))
            return MEMBER;
        else
            return null;
    }

    /**
     * Gets the {@link WebServiceFeature} corresponding to the namespace URI of
     * WS-Addressing policy assertion in the WSDL. <code>enabled</code> and
     * <code>required</code> are used to initialize the value of the feature.
     *
     * @param nsUri namespace URI of the WS-Addressing policy assertion in the WSDL
     * @param enabled true if feature is to be enabled, false otherwise
     * @param required true if feature is required, false otherwise. Corresponds
     *          to wsdl:required on the extension/assertion.
     * @return WebServiceFeature corresponding to the assertion namespace URI
     * @throws WebServiceException if an unsupported namespace URI is passed
     */
    public static @NotNull WebServiceFeature getFeature(String nsUri, boolean enabled, boolean required) {
        if (nsUri == W3C.policyNsUri)
            return new AddressingFeature(enabled, required);
        else if (nsUri == MEMBER.policyNsUri)
            return new MemberSubmissionAddressingFeature(enabled, required);
        else
            throw new WebServiceException("Unsupported namespace URI: " + nsUri);
    }

    /**
     * Gets the corresponding {@link AddressingVersion} instance from the
     * EPR class.
     */
    public static @NotNull AddressingVersion fromSpecClass(Class<? extends EndpointReference> eprClass) {
        if(eprClass==W3CEndpointReference.class)
            return W3C;
        if(eprClass==MemberSubmissionEndpointReference.class)
            return MEMBER;
        throw new WebServiceException("Unsupported EPR type: "+eprClass);
    }

    /**
     * Returns true if the WebServiceFeature is either a {@link AddressingFeature} or
     * {@link MemberSubmissionAddressingFeature} and is required.
     *
     * @param wsf The WebServiceFeature encaps
     * @throws WebServiceException if <code>wsf</code> does not contain either {@link AddressingFeature} or
     * {@link MemberSubmissionAddressingFeature}
     * @return true if <code>wsf</code> requires WS-Addressing
     */
    public static boolean isRequired(WebServiceFeature wsf) {
        if (wsf.getID().equals(AddressingFeature.ID)) {
            return ((AddressingFeature)wsf).isRequired();
        } else if (wsf.getID().equals(MemberSubmissionAddressingFeature.ID)) {
            return ((MemberSubmissionAddressingFeature)wsf).isRequired();
        } else
            throw new WebServiceException("WebServiceFeature not an Addressing feature: "+ wsf.getID());
    }

    /**
     * Returns true if <code>binding</code> contains either a {@link AddressingFeature} or
     * {@link MemberSubmissionAddressingFeature} and is required.
     *
     * @param binding The binding
     * @return true if <code>binding</code> requires WS-Addressing
     */
    public static boolean isRequired(WSBinding binding) {
        WebServiceFeature wsf = binding.getFeature(AddressingFeature.ID);
        if (wsf == null)
            wsf = binding.getFeature(MemberSubmissionAddressingFeature.ID);

        if (wsf == null)
            return false;
        else
            return isRequired(wsf);
    }

    /**
     * Returns true if <code>binding</code> contains either a {@link AddressingFeature} or
     * {@link MemberSubmissionAddressingFeature} and is enabled.
     *
     * @param binding The binding
     * @return true if WS-Addressing is enabled for <code>binding</code>.
     */
    public static boolean isEnabled(WSBinding binding) {
        return binding.isFeatureEnabled(MemberSubmissionAddressingFeature.ID) ||
                binding.isFeatureEnabled(AddressingFeature.ID);
    }
}
