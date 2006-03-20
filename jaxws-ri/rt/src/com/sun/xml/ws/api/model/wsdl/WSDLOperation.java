package com.sun.xml.ws.api.model.wsdl;

import com.sun.xml.ws.api.model.wsdl.WSDLExtensible;

import javax.xml.namespace.QName;

/**
 * Provides abstraction of wsdl:portType/wsdl:operation.
 *
 * @author Vivek Pandey
 */
public interface WSDLOperation extends WSDLObject, WSDLExtensible {
    /**
     * Gets the name of the wsdl:portType/wsdl:operation@name attribute value as local name and wsdl:definitions@targetNamespace
     * as the namespace uri.
     */
    QName getName();

    /**
     * Gets the wsdl:input of this operation
     */
    WSDLInput getInput();

    /**
     * Gets the wsdl:output of this operation.
     *
     * @return
     *      null if this is an one-way operation.
     */
    WSDLOutput getOutput();



    /**
     * Returns true if this operation is an one-way operation.
     */
    boolean isOneWay();

    /**
     * Gets the {link WSDLFault} corresponding to wsdl:fault of this operation.
     */
    Iterable<? extends WSDLFault> getFaults();

    /**
     * Gives {@link WSDLFault} for the given soap fault detail value.
     *
     * <pre>
     *
     * Given a wsdl fault:
     *
     * &lt;wsdl:message nae="faultMessage">
     *  &lt;wsdl:part name="fault" element="<b>ns:myException</b>/>
     * &lt;/wsdl:message>
     *
     * &lt;wsdl:portType>
     *  &lt;wsdl:operation ...>
     *      &lt;wsdl:fault name="aFault" message="faultMessage"/>
     *  &lt;/wsdl:operation>
     * &lt;wsdl:portType>
     *
     *
     * For example given a soap 11 soap message:
     *
     * &lt;soapenv:Fault>
     *      ...
     *      &lt;soapenv:detail>
     *          &lt;<b>ns:myException</b>>
     *              ...
     *          &lt;/ns:myException>
     *      &lt;/soapenv:detail>
     *
     * QName faultQName = new QName(ns, "myException");
     * WSDLFault wsdlFault  = getFault(faultQName);
     *
     * The above call will return a WSDLFault that abstracts wsdl:portType/wsdl:operation/wsdl:fault.
     *
     * </pre>
     *
     * @param faultDetailName tag name of the element inside soaenv:Fault/detail/, must be non-null.
     * @return returns null if a wsdl fault corresponding to the detail entry name not found.
     */
    WSDLFault getFault(QName faultDetailName);
}
