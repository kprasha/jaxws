package com.sun.xml.ws.api.model.wsdl;

import com.sun.xml.ws.api.model.wsdl.BoundPortType;
import com.sun.xml.ws.api.model.wsdl.Extensible;

import javax.xml.namespace.QName;

/**
 * Abstracts wsdl:service/wsdl:port
 *
 * @author Vivek Pandey
 */
public interface Port extends Extensible {
    /**
     * Gets wsdl:port@name attribute value as local name and wsdl:definitions@targetNamespace
     * as the namespace uri.
     */
    QName getName();

    /**
     * Gets wsdl:service/wsdl:port@binding attribute value as (@link QName}. This name can be used to find a
     * {@link BoundPortType} from {@link WSDLModel#getBinding(javax.xml.namespace.QName)}
     */
    @Deprecated // redundant?
    QName getBindingName();

    /**
     * Gets {@link BoundPortType} associated with the {@link Port}.
     */
    BoundPortType getBinding();

    /**
     * Gets the wsdl:service/wsdl:port@address attribute value.
     */
    String getAddress();
}
