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

package com.sun.xml.ws.wsdl.parser;

import javax.xml.namespace.QName;

/**
 * Constants for Member Submission WS-Addressing version
 *
 * @author Arun Gupta
 */
public interface MemberSubmissionAddressingConstants {
    public static final String WSA_NAMESPACE_NAME = "http://schemas.xmlsoap.org/ws/2004/08/addressing";
    public static final String WSA_NAMESPACE_WSDL_NAME = WSA_NAMESPACE_NAME;
    public static final String WSA_NAMESPACE_PREFIX = "wsa";
    public static final String WSA_NAMESPACE_WSDL_PREFIX = "wsa";

    public static final String WSA_REFERENCEPROPERTIES_NAME = "ReferenceProperties";
    public static final QName WSA_REFERENCEPROPERTIES_QNAME = new QName(WSA_NAMESPACE_NAME, WSA_REFERENCEPROPERTIES_NAME);

    public static final String WSA_REFERENCEPARAMETERS_NAME = "ReferenceParameters";
    public static final QName WSA_REFERENCEPARAMETERS_QNAME = new QName(WSA_NAMESPACE_NAME, WSA_REFERENCEPARAMETERS_NAME);

    public static final String WSA_ISREFPARAM_NAME = "isReferenceParameter";
    public static final QName WSA_ISREFPARAM_QNAME = new QName(WSA_NAMESPACE_NAME, WSA_ISREFPARAM_NAME);

    public static final String WSA_ADDRESS_NAME = "Address";
    public static final QName WSA_ADDRESS_QNAME = new QName(WSA_NAMESPACE_NAME, WSA_ADDRESS_NAME);

    public static final String WSA_METADATA_NAME = WSA_REFERENCEPARAMETERS_NAME;
    public static final QName WSA_METADATA_QNAME = new QName(WSA_NAMESPACE_NAME, WSA_METADATA_NAME);

    public static final QName WSA_FROM_QNAME = new QName(WSA_NAMESPACE_NAME, "From");
    public static final QName WSA_TO_QNAME = new QName(WSA_NAMESPACE_NAME, "To");
    public static final QName WSA_REPLYTO_QNAME = new QName(WSA_NAMESPACE_NAME, "ReplyTo");
    public static final QName WSA_FAULTTO_QNAME = new QName(WSA_NAMESPACE_NAME, "FaultTo");
    public static final QName WSA_ACTION_QNAME = new QName(WSA_NAMESPACE_NAME, "Action");
    public static final QName WSA_MESSAGEID_QNAME = new QName(WSA_NAMESPACE_NAME, "MessageID");
    public static final QName IS_REFERENCE_PARAMETER_QNAME = new QName(WSA_NAMESPACE_NAME, "IsReferenceParameter");

    public static final String WSA_RELATIONSHIP_REPLY = WSA_NAMESPACE_NAME + "/reply";
    public static final QName WSA_RELATESTO_QNAME = new QName(WSA_NAMESPACE_NAME, "RelatesTo");
    public static final QName WSA_RELATIONSHIPTYPE_QNAME = new QName(WSA_NAMESPACE_NAME, "RelationshipType");

    public static final String WSA_ANONYMOUS_ADDRESS = WSA_NAMESPACE_NAME + "/role/anonymous";
    public static final String WSA_NONE_ADDRESS = "";

    public static final String WSA_DEFAULT_FAULT_ACTION = WSA_NAMESPACE_NAME + "/fault";

    public static final String WSAW_ACTION_NAME = "Action";
    public static final QName WSAW_ACTION_QNAME = new QName(WSA_NAMESPACE_WSDL_NAME, WSAW_ACTION_NAME);

    public static final String WSDL_INSTANCE_NAMESPACE_NAME = "http://www.w3.org/2004/08/wsdl-instance";

    public static final QName INVALID_MAP_QNAME = new QName(WSA_NAMESPACE_NAME, "InvalidMessageInformationHeader");
    public static final QName MAP_REQUIRED_QNAME = new QName(WSA_NAMESPACE_NAME, "MessageAddressingPropertyRequired");
    public static final QName DESTINATION_UNREACHABLE_QNAME = new QName(WSA_NAMESPACE_NAME, "DestinationUnreachable");
    public static final QName ACTION_NOT_SUPPORTED_QNAME = new QName(WSA_NAMESPACE_NAME, "ActionNotSupported");
    public static final QName ENDPOINT_UNAVAILABLE_QNAME = new QName(WSA_NAMESPACE_NAME, "EndpointUnavailable");

    public static final String ACTION_NOT_SUPPORTED_TEXT = "The \"%s\" cannot be processed at the receiver.";
    public static final String DESTINATION_UNREACHABLE_TEXT = "No route can be determined to reach the destination role defined by the WS-Addressing To.";
    public static final String ENDPOINT_UNAVAILABLE_TEXT = "The endpoint is unable to process the message at this time.";
    public static final String INVALID_MAP_TEXT = "A message information header is not valid and the message cannot be processed.";
    public static final String MAP_REQUIRED_TEXT = "A required message information header, To, MessageID, or Action, is not present.";

    public static final QName PROBLEM_ACTION_QNAME = new QName(WSA_NAMESPACE_NAME, "ProblemAction");
    public static final QName PROBLEM_HEADER_QNAME_QNAME = new QName(WSA_NAMESPACE_NAME, "ProblemHeaderQName");
    public static final QName FAULT_DETAIL_QNAME = new QName(WSA_NAMESPACE_NAME, "FaultDetail");
}
