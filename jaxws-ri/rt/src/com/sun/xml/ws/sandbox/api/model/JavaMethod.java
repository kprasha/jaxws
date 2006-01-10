package com.sun.xml.ws.sandbox.api.model;

import com.sun.xml.ws.pept.presentation.MEP;
import com.sun.xml.bind.api.TypeReference;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @author Vivek Pandey
 */
public interface JavaMethod {
    /**
     * @return Returns the method.
     */
    Method getMethod();

    /**
     * @return Returns the mep.
     */
    MEP getMEP();

    /**
     * @return the Binding object
     */
    Object getBinding();

    String getOperationName();

    /**
     * @return returns unmodifiable list of request parameters
     */
    List<Parameter> getRequestParameters();

    /**
     * @return returns unmodifiable list of response parameters
     */
    List<Parameter> getResponseParameters();

    /**
     * @return Returns number of java method parameters - that will be all the
     *         IN, INOUT and OUT holders
     */
    int getInputParametersCount();

    /**
     * @param exceptionClass
     * @return CheckedException corresponding to the exceptionClass. Returns
     *         null if not found.
     */
    CheckedException getCheckedException(Class exceptionClass);

    /**
     * @return a list of checked Exceptions thrown by this method
     */
    List<CheckedException> getCheckedExceptions();

    /**
     * @param detailType
     * @return Gets the CheckedException corresponding to detailType. Returns
     *         null if no CheckedExcpetion with the detailType found.
     */
    CheckedException getCheckedException(TypeReference detailType);

    /**
     * Returns if the java method MEP is async
     * @return if this is an Asynch MEP
     */
    boolean isAsync();
}
