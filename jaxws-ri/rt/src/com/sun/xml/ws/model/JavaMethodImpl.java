/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 * 
 * You can obtain a copy of the license at
 * https://jwsdp.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL
 * HEADER in each file and include the License file at
 * https://jwsdp.dev.java.net/CDDLv1.0.html  If applicable,
 * add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your
 * own identifying information: Portions Copyright [yyyy]
 * [name of copyright owner]
 */
package com.sun.xml.ws.model;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sun.xml.bind.api.TypeReference;
import com.sun.xml.ws.model.soap.SOAPBindingImpl;
import com.sun.xml.ws.pept.presentation.MEP;
import com.sun.xml.ws.api.model.JavaMethod;
import com.sun.xml.ws.api.model.Parameter;
import com.sun.xml.ws.api.model.CheckedException;
import com.sun.xml.ws.api.model.wsdl.WSDLBoundPortType;
import com.sun.xml.ws.api.model.wsdl.WSDLBoundOperation;
import com.sun.xml.ws.api.model.soap.SOAPBinding;

import javax.xml.namespace.QName;

/**
 * Build this runtime model using java SEI and annotations
 *
 * @author Vivek Pandey
 */
public class JavaMethodImpl implements JavaMethod {
    /**
     *
     */
    public JavaMethodImpl(Method method) {
        this.method = method;
    }

    /**
     * @return Returns the method.
     */
    public Method getMethod() {
        return method;
    }

    /**
     * @return Returns the mep.
     */
    public MEP getMEP() {
        return mep;
    }

    /**
     * @param mep
     *            The mep to set.
     */
    void setMEP(MEP mep) {
        this.mep = mep;
    }

    /**
     * @return the Binding object
     */
    public SOAPBinding getBinding() {
        if (binding == null)
            return new SOAPBindingImpl();
        return binding;
    }

    /**
     * @param binding
     */
    void setBinding(SOAPBinding binding) {
        this.binding = binding;
    }

    public WSDLBoundOperation getOperation() {
        assert wsdlOperation!=null;
        return wsdlOperation;
    }

    public void setOperationName(String name) {
        this.operationName = name;
    }

    public String getOperationName() {
        return operationName;
    }

    /**
     * @return returns unmodifiable list of request parameters
     */
    public List<Parameter> getRequestParameters() {
        return unmReqParams;
    }

    /**
     * @return returns unmodifiable list of response parameters
     */
    public List<Parameter> getResponseParameters() {
        return unmResParams;
    }

    /**
     * @param p
     */
    void addParameter(ParameterImpl p) {
        if (p.isIN() || p.isINOUT()) {
            if (requestParams.contains(p)) {
                // TODO throw exception
            }
            requestParams.add(p);
        }

        if (p.isOUT() || p.isINOUT()) {
            // this check is only for out parameters
            if (requestParams.contains(p)) {
                // TODO throw exception
            }
            responseParams.add(p);
        }
    }

    void addRequestParameter(ParameterImpl p){
        if (p.isIN() || p.isINOUT()) {
            requestParams.add(p);
        }
    }

    void addResponseParameter(ParameterImpl p){
        if (p.isOUT() || p.isINOUT()) {
            responseParams.add(p);
        }
    }

    /**
     * @return Returns number of java method parameters - that will be all the
     *         IN, INOUT and OUT holders
     */
    public int getInputParametersCount() {
        int count = 0;
        for (Parameter param : requestParams) {
            if (param.isWrapperStyle()) {
                count += ((WrapperParameter) param).getWrapperChildren().size();
            } else {
                count++;
            }
        }

        for (Parameter param : responseParams) {
            if (param.isWrapperStyle()) {
                for (Parameter wc : ((WrapperParameter) param).getWrapperChildren()) {
                    if (!wc.isResponse() && wc.isOUT()) {
                        count++;
                    }
                }
            } else if (!param.isResponse() && param.isOUT()) {
                count++;
            }
        }

        return count;
    }

    /**
     * @param ce
     */
    void addException(CheckedException ce) {
        if (!exceptions.contains(ce))
            exceptions.add(ce);
    }

    /**
     * @param exceptionClass
     * @return CheckedException corresponding to the exceptionClass. Returns
     *         null if not found.
     */
    public CheckedException getCheckedException(Class exceptionClass) {
        for (CheckedException ce : exceptions) {
            if (ce.getExcpetionClass().equals(exceptionClass))
                return ce;
        }
        return null;
    }

    /**
     * @return a list of checked Exceptions thrown by this method
     */
    public List<CheckedException> getCheckedExceptions(){
        return Collections.unmodifiableList(exceptions);
    }
    /**
     * @param detailType
     * @return Gets the CheckedException corresponding to detailType. Returns
     *         null if no CheckedExcpetion with the detailType found.
     */
    public CheckedException getCheckedException(TypeReference detailType) {
        for (CheckedException ce : exceptions) {
            TypeReference actual = ce.getDetailType();
            if (actual.tagName.equals(detailType.tagName)
                    && actual.type.getClass().getName()
                            .equals(detailType.type.getClass().getName())) {
                return ce;
            }
        }
        return null;
    }

    /**
     * Returns if the java method MEP is async
     * @return if this is an Asynch MEP
     */
    public boolean isAsync(){
        return mep.isAsync;
    }

    /*package*/ void freeze(WSDLBoundPortType portType) {
        this.wsdlOperation = portType.get(new QName(portType.getName().getNamespaceURI(),operationName));
        // TODO: replace this with proper error handling
        if(wsdlOperation ==null)
            throw new Error("Undefined operation name "+operationName);
    }

    private List<CheckedException> exceptions = new ArrayList<CheckedException>();
    private Method method;
    /*package*/ final List<ParameterImpl> requestParams = new ArrayList<ParameterImpl>();
    /*package*/ final List<ParameterImpl> responseParams = new ArrayList<ParameterImpl>();
    private final List<Parameter> unmReqParams =
            Collections.<Parameter>unmodifiableList(requestParams);
    private final List<Parameter> unmResParams =
            Collections.<Parameter>unmodifiableList(responseParams);
    private SOAPBinding binding;
    private MEP mep;
    private String operationName;
    private WSDLBoundOperation wsdlOperation;
}

