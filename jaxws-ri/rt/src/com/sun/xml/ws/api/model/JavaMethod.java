package com.sun.xml.ws.api.model;

import com.sun.xml.bind.api.TypeReference;
import com.sun.xml.ws.api.model.wsdl.WSDLBoundOperation;
import com.sun.xml.ws.pept.presentation.MEP;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Abstracts the annotated {@link Method} of a SEI.
 *
 * @author Vivek Pandey
 */
public interface JavaMethod {
    /**
     * @return Returns the java {@link Method}
     */
    Method getMethod();

    /**
     * @return Returns the {@link MEP}.
     */
    MEP getMEP();

    /**
     * Binding object - a {@link com.sun.xml.ws.api.model.soap.SOAPBinding} isntance.
     *
     * @return the Binding object
     */
//    Object getBinding();

    /**
     * The {@link WSDLBoundOperation} that this method represents.
     *
     * @return
     *      always non-null.
     */
//    WSDLBoundOperation getOperation();

    /**
     * Request parameters can be {@link com.sun.xml.ws.api.model.Mode#IN} or
     * {@link com.sun.xml.ws.api.model.Mode#INOUT} and these parameters go in a request message on-the-wire.
     * Further a Parameter can be instance of {@link com.sun.xml.ws.model.WrapperParameter} when
     * the operation is wrapper style.
     *
     * @return returns unmodifiable list of request parameters
     */
//    List<? extends Parameter> getRequestParameters();

    /**
     * Response parameters go in the response message on-the-wire and can be of
     * {@link com.sun.xml.ws.api.model.Mode#OUT} or
     * {@link com.sun.xml.ws.api.model.Mode#INOUT}
     * @return returns unmodifiable list of response parameters
     */
//    List<? extends Parameter> getResponseParameters();

    /**
     * @return Returns number of java method parameters - that will be all the
     *         IN, INOUT and OUT holders
     */
//    int getInputParametersCount();

   /**
     * @param exceptionClass
     * @return CheckedException corresponding to the exceptionClass. Returns
     *         null if not found.
     */
//    CheckedException getCheckedException(Class exceptionClass);

    /**
     * @return a list of checked Exceptions thrown by this method
     */
//    List<? extends CheckedException> getCheckedExceptions();

    /**
     * @param detailType
     * @return Gets the CheckedException corresponding to detailType. Returns
     *         null if no CheckedExcpetion with the detailType found.
     */
//    CheckedException getCheckedException(TypeReference detailType);

    /**
     * Returns if the java method MEP is async
     * @return if this is an Asynch MEP
     */
//    boolean isAsync();
}
