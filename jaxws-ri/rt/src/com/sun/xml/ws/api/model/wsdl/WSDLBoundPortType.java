package com.sun.xml.ws.api.model.wsdl;

import com.sun.xml.ws.api.model.ParameterBinding;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.message.Message;

import javax.xml.namespace.QName;
import javax.xml.ws.soap.SOAPBinding;
import javax.jws.WebParam.Mode;

/**
 * {@link WSDLPortType} bound with a specific binding.
 *
 * @author Vivek Pandey
 */
public interface WSDLBoundPortType extends WSDLObject, WSDLExtensible {
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

    /**
     * Gets the bound operation in this port for a tag name. Here the operation would be the one whose
     * input part descriptor bound to soap:body is same as the tag name except for rpclit where the tag
     * name would be {@link WSDLBoundOperation#getName()}.
     *
     * <p>
     * If you have a {@link Message} and trying to figure out which operation it belongs to,
     * always use {@link Message#getOperation}, as that performs better.
     *
     * <p>
     * For example this can be used in the case when a message receipient can get the
     * {@link WSDLBoundOperation} from the payload tag name.
     *
     * <p>
     * namespaceUri and the local name both can be null to get the WSDLBoundOperation that has empty body -
     * there is no payload. According to BP 1.1 in a port there can be at MOST one operation with empty body.
     * Its an error to have namespace URI non-null but local name as null.
     *
     * @param namespaceUri namespace of the payload element.
     * @param localName local name of the payload
     * @return null if no operation with the given tag name is found
     * @throws NullPointerException if localName is null and namespaceUri is not.
     */
    WSDLBoundOperation getOperation(String namespaceUri, String localName);
}
