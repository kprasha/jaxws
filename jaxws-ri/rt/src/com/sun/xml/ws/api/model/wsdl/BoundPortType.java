package com.sun.xml.ws.api.model.wsdl;

import com.sun.xml.ws.api.model.Mode;
import com.sun.xml.ws.api.model.ParameterBinding;

import javax.xml.namespace.QName;
import java.util.Iterator;

/**
 * @author Vivek Pandey
 */
public interface BoundPortType extends Extensible {
    /**
     * Gets the name of the wsdl:binding@name attribute value as local name and wsdl:definitions@targetNamespace
     * as the namespace uri.
     */
    QName getName();

    /**
     * Gets the {@link BoundOperation} for a given operation name
     *
     * @param operationName non-null operationName
     * @return null if a {@link BoundOperation} is not found
     */
    public BoundOperation get(String operationName);

    /**
     * Gets the wsdl:binding@type value, same as {@link PortType#getName()}
     */
    QName getPortTypeName();

    /**
     * Gets the {@link PortType} associated with the wsdl:binding
     */
    PortType getPortType();

    /**
     * Gets the {@link BoundOperation}s
     */
    Iterator<BoundOperation> getBindingOperations();

    /**
     * Returns {@link javax.xml.ws.soap.SOAPBinding#SOAP11HTTP_BINDING} or {@link javax.xml.ws.soap.SOAPBinding#SOAP12HTTP_BINDING}. This
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
    ParameterBinding getBinding(String operation, String part, Mode mode);

    /**
     * Gets mime:content@part value which is the MIME type for a given operation, part and {@link Mode}.
     *
     * @param operation wsdl:operation@name value. Must be non-null.
     * @param part      wsdl:part@name such as value of soap:header@part. Must be non-null.
     * @param mode      {@link Mode#IN} or {@link Mode@OUT}. Must be non-null.
     * @return null if the binding could not be resolved for the part.
     */
    String getMimeType(String operation, String part, Mode mode);
}
