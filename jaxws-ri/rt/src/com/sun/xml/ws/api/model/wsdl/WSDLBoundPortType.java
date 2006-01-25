package com.sun.xml.ws.api.model.wsdl;

import com.sun.xml.ws.api.model.Mode;
import com.sun.xml.ws.api.model.ParameterBinding;

import javax.xml.namespace.QName;
import javax.xml.ws.soap.SOAPBinding;

/**
 * {@link WSDLPortType} bound with a specific binding.
 *
 * @author Vivek Pandey
 */
public interface WSDLBoundPortType extends Extensible {
    /**
     * Gets the name of the wsdl:binding@name attribute value as local name and wsdl:definitions@targetNamespace
     * as the namespace uri.
     */
    QName getName();

    /**
     * Gets the {@link WSDLBoundOperation} for a given operation name
     *
     * @param operationName non-null operationName
     * @return null if a {@link WSDLBoundOperation} is not found
     */
    public WSDLBoundOperation get(QName operationName);

    /**
     * Gets the wsdl:binding@type value, same as {@link WSDLPortType#getName()}
     */
    QName getPortTypeName();

    /**
     * Gets the {@link WSDLPortType} associated with the wsdl:binding
     */
    WSDLPortType getPortType();

    /**
     * Gets the {@link WSDLBoundOperation}s
     */
    Iterable<? extends WSDLBoundOperation> getBindingOperations();

    /**
     * Returns {@link SOAPBinding#SOAP11HTTP_BINDING} or {@link SOAPBinding#SOAP12HTTP_BINDING}. This
     * would typically determined by the binding extension elements in wsdl:binding.
     */
    String getBindingId();

    /**
     * Gets the {@link ParameterBinding} for a given operation, part name and the direction - IN/OUT
     *
     * @param operation wsdl:operation@name value. Must be non-null.
     * @param part      wsdl:part@name such as value of soap:header@part. Must be non-null.
     * @param mode      {@link Mode#IN} or {@link Mode@OUT}. Must be non-null.
     * @return null if the binding could not be resolved for the part.
     */
    ParameterBinding getBinding(QName operation, String part, Mode mode);

    /**
     * Gets mime:content@part value which is the MIME type for a given operation, part and {@link Mode}.
     *
     * @param operation wsdl:operation@name value. Must be non-null.
     * @param part      wsdl:part@name such as value of soap:header@part. Must be non-null.
     * @param mode      {@link Mode#IN} or {@link Mode@OUT}. Must be non-null.
     * @return null if the binding could not be resolved for the part.
     */
    String getMimeType(QName operation, String part, Mode mode);
}
