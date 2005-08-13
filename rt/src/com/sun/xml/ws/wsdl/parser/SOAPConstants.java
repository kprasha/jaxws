/**
 * $Id: SOAPConstants.java,v 1.1 2005-08-13 19:30:37 vivekp Exp $
 */

/**
 * Copyright 2005 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.sun.xml.ws.wsdl.parser;

import com.sun.xml.ws.encoding.soap.streaming.SOAPNamespaceConstants;
import com.sun.xml.ws.encoding.soap.streaming.SOAP12NamespaceConstants;

import javax.xml.namespace.QName;

public interface SOAPConstants {

    // namespace URIs
    public static final String URI_ENVELOPE = SOAPNamespaceConstants.ENVELOPE;
    public static final String URI_ENVELOPE12 = SOAP12NamespaceConstants.ENVELOPE;

    public static final String NS_WSDL_SOAP =
        "http://schemas.xmlsoap.org/wsdl/soap/";

    public static final String NS_WSDL_SOAP12 =
        "http://schemas.xmlsoap.org/wsdl/soap12/";

    public static final String NS_SOAP_ENCODING = "http://schemas.xmlsoap.org/soap/encoding/";

    // other URIs
    public final String URI_SOAP_TRANSPORT_HTTP =
        "http://schemas.xmlsoap.org/soap/http";

    // QNames
    public static final QName QNAME_ADDRESS =
        new QName(NS_WSDL_SOAP, "address");
    public static final QName QNAME_BINDING =
        new QName(NS_WSDL_SOAP, "binding");
    public static final QName QNAME_BODY = new QName(NS_WSDL_SOAP, "body");
    public static final QName QNAME_FAULT = new QName(NS_WSDL_SOAP, "fault");
    public static final QName QNAME_HEADER = new QName(NS_WSDL_SOAP, "header");
    public static final QName QNAME_HEADERFAULT =
        new QName(NS_WSDL_SOAP, "headerfault");
    public static final QName QNAME_OPERATION =
        new QName(NS_WSDL_SOAP, "operation");
    public static final QName QNAME_MUSTUNDERSTAND =
        new QName(URI_ENVELOPE, "mustUnderstand");


}
