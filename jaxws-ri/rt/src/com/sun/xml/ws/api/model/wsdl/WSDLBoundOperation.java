package com.sun.xml.ws.api.model.wsdl;

import javax.jws.WebParam.Mode;
import com.sun.xml.ws.api.model.ParameterBinding;

import javax.xml.namespace.QName;
import java.util.Map;

/**
 * Abstracts wsdl:binding/wsdl:operation. It can be used to determine the parts and their binding.
 *
 * @author Vivek Pandey
 */
public interface WSDLBoundOperation extends WSDLObject, WSDLExtensible {
    /**
     * Short-cut for {@code getOperation().getName()}
     */
    QName getName();

    /**
     * Gets the wsdl:portType/wsdl:operation model - {@link WSDLOperation},
     * associated with this binding operation.
     * @return non-null {@link WSDLOperation}
     */
    public WSDLOperation getOperation();
}
