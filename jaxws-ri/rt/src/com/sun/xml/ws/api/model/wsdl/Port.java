package com.sun.xml.ws.api.model.wsdl;

import com.sun.xml.ws.api.model.wsdl.WSDLBinding;
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
     * {@link WSDLBinding} from {@link WSDLModel#getBinding(javax.xml.namespace.QName)}
     */
    QName getBindingName();

    /**
     * Gets {@link WSDLBinding} associated with the {@link Port}.
     */
    WSDLBinding getBinding();

    /**
     * Gets the wsdl:service/wsdl:port@address attribute value.
     */
    String getAddress();
}
