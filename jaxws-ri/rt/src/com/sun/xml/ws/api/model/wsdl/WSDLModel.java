package com.sun.xml.ws.api.model.wsdl;


import javax.xml.namespace.QName;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.soap.SOAPBinding;
import java.util.List;
import java.util.Map;

/**
 * Provides abstraction of a wsdl document.
 *
 * @author Vivek Pandey
 */
public interface WSDLModel {

    /**
     * Gets {@link PortType} that models <code>wsdl:portType</code>
     *
     * @param name non-null quaified name of wsdl:message, where the localName is the value of <code>wsdl:portType@name</code> and
     *             the namespaceURI is the value of wsdl:definitions@targetNamespace
     * @return A {@link com.sun.xml.ws.api.model.wsdl.PortType} or null if no wsdl:portType found.
     */
    PortType getPortType(QName name);

    /**
     * Gets {@link BoundPortType} that models <code>wsdl:binding</code>
     *
     * @param name non-null quaified name of wsdl:binding, where the localName is the value of <code>wsdl:binding@name</code> and
     *             the namespaceURI is the value of wsdl:definitions@targetNamespace
     * @return A {@link BoundPortType} or null if no wsdl:binding found
     */
    BoundPortType getBinding(QName name);

    /**
     * Give a {@link BoundPortType} for the given wsdl:service and wsdl:port names.
     *
     * @param serviceName non-null service QName
     * @param portName    non-null port QName
     * @return A {@link BoundPortType}. null if the Binding for the given wsd:service and wsdl:port name are not
     *         found.
     */
    BoundPortType getBinding(QName serviceName, QName portName);

    /**
     * Returns the bindings for the given bindingId
     *
     * @param service   non-null service
     * @param bindingId non-null - can be either {@link SOAPBinding#SOAP11HTTP_BINDING} or
     *                  {@link SOAPBinding#SOAP12HTTP_BINDING}
     * @return empty List if no wsdl:binding corresponding to the bindingId is found.
     */
    List<BoundPortType> getBindings(Service service, String bindingId);

    /**
     * Gets {@link Service} that models <code>wsdl:service</code>
     *
     * @param name non-null quaified name of wsdl:service, where the localName is the value of <code>wsdl:service@name</code> and
     *             the namespaceURI is the value of wsdl:definitions@targetNamespace
     * @return A {@link Service} or null if no wsdl:service found
     */
    Service getService(QName name);

    /**
     * Gives a {@link Map} of wsdl:portType {@link QName} and {@link PortType}
     *
     * @return an empty Map if the wsdl document has no wsdl:portType
     */
    Map<QName, PortType> getPortTypes();

    /**
     * Gives a {@link Map} of wsdl:binding {@link QName} and {@link BoundPortType}
     *
     * @return an empty Map if the wsdl document has no wsdl:binding
     */
    Map<QName, BoundPortType> getBindings();

    /**
     * Gives a {@link Map} of wsdl:service qualified name and {@link com.sun.xml.ws.api.model.wsdl.Service}
     *
     * @return an empty Map if the wsdl document has no wsdl:service
     */
    Map<QName, Service> getServices();

    /**
     * Gets the bound operation for a service, port and a tag name. the
     * {@link BoundOperation} will provide the operation parts and the respective
     * bindings. Here the operation would be the one whose input part descriptor is same as
     * the tag name except for rpclit where the tag name would be {@link BoundOperation@getName()}. 
     * <p>
     * For example this can be used in the case when a message receipient can get the
     * {@link BoundOperation} from the payload tag name.
     *
     * @param serviceName  non-null service name
     * @param portName     non-null port name
     * @param tag          The payload tag name.
     * @return null if the operation is not found
     * @throws NullPointerException if either of serviceName, portName or operationName is null.
     * @see com.sun.xml.ws.api.model.ParameterBinding
     */
    BoundOperation getOperation(QName serviceName, QName portName, QName tag);

    /**
     * Gives the binding Id for a given wsdl:port and wsdl:service name. The binding Id can be either
     * {@link SOAPBinding#SOAP11HTTP_BINDING} or {@link SOAPBinding#SOAP12HTTP_BINDING}
     * of the given service and port.
     *
     * @param service qualified name of wsdl:service. Must be non-null.
     * @param port    qualified name of wsdl:port. Must be non-null.
     * @return The binding ID associated with the serivce and port.
     * @throws WebServiceException If the binding correponding to the service or port is unkown (other than
     *                             {@link SOAPBinding#SOAP11HTTP_BINDING} or
     *                             {@link SOAPBinding#SOAP12HTTP_BINDING})
     */
    String getBindingId(QName service, QName port) throws WebServiceException;
}
