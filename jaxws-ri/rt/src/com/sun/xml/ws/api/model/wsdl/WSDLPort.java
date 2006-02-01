package com.sun.xml.ws.api.model.wsdl;

import com.sun.xml.ws.api.model.wsdl.WSDLBoundPortType;
import com.sun.xml.ws.api.model.wsdl.WSDLExtensible;

import javax.xml.namespace.QName;

/**
 * Abstracts wsdl:service/wsdl:port
 *
 * @author Vivek Pandey
 */
public interface WSDLPort extends WSDLObject, WSDLExtensible {
    /**
     * Gets wsdl:port@name attribute value as local name and wsdl:definitions@targetNamespace
     * as the namespace uri.
     */
    QName getName();

    /**
     * Gets wsdl:service/wsdl:port@binding attribute value as (@link QName}. This name can be used to find a
     * {@link WSDLBoundPortType} from {@link WSDLModel#getBinding(javax.xml.namespace.QName)}
     */
    @Deprecated // redundant?
    QName getBindingName();

    /**
     * Gets {@link WSDLBoundPortType} associated with the {@link WSDLPort}.
     */
    WSDLBoundPortType getBinding();

    /**
     * Gets the wsdl:service/wsdl:port@address attribute value.
     */
    String getAddress();
}
