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

import com.sun.xml.bind.api.TypeReference;
import com.sun.xml.ws.api.model.JavaMethod;
import com.sun.xml.ws.api.model.soap.SOAPBinding;
import com.sun.xml.ws.api.model.wsdl.WSDLBoundOperation;
import com.sun.xml.ws.model.soap.SOAPBindingImpl;
import com.sun.xml.ws.model.wsdl.WSDLPortImpl;
import com.sun.xml.ws.pept.presentation.MEP;

import javax.xml.namespace.QName;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Build this runtime model using java SEI and annotations
 *
 * @author Vivek Pandey
 */
public final class JavaMethodImpl implements JavaMethod {

    private final List<CheckedExceptionImpl> exceptions = new ArrayList<CheckedExceptionImpl>();
    private final Method method;
    /*package*/ final List<ParameterImpl> requestParams = new ArrayList<ParameterImpl>();
    /*package*/ final List<ParameterImpl> responseParams = new ArrayList<ParameterImpl>();
    private final List<ParameterImpl> unmReqParams = Collections.unmodifiableList(requestParams);
    private final List<ParameterImpl> unmResParams = Collections.unmodifiableList(responseParams);
    private SOAPBinding binding;
    private MEP mep;
    private String operationName;
    private WSDLBoundOperation wsdlOperation;
    /*package*/ final AbstractSEIModelImpl owner;

    public JavaMethodImpl(AbstractSEIModelImpl owner, Method method) {
        this.owner = owner;
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
    public List<ParameterImpl> getRequestParameters() {
        return unmReqParams;
    }

    /**
     * @return returns unmodifiable list of response parameters
     */
    public List<ParameterImpl> getResponseParameters() {
        return unmResParams;
    }

    void addParameter(ParameterImpl p) {
        if (p.isIN() || p.isINOUT()) {
            assert !requestParams.contains(p);
            requestParams.add(p);
        }

        if (p.isOUT() || p.isINOUT()) {
            // this check is only for out parameters
            assert !responseParams.contains(p);
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
     *
     * @deprecated no longer use in the new architecture
     */
    public int getInputParametersCount() {
        int count = 0;
        for (ParameterImpl param : requestParams) {
            if (param.isWrapperStyle()) {
                count += ((WrapperParameter) param).getWrapperChildren().size();
            } else {
                count++;
            }
        }

        for (ParameterImpl param : responseParams) {
            if (param.isWrapperStyle()) {
                for (ParameterImpl wc : ((WrapperParameter) param).getWrapperChildren()) {
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
    void addException(CheckedExceptionImpl ce) {
        if (!exceptions.contains(ce))
            exceptions.add(ce);
    }

    /**
     * @param exceptionClass
     * @return CheckedException corresponding to the exceptionClass. Returns
     *         null if not found.
     */
    public CheckedExceptionImpl getCheckedException(Class exceptionClass) {
        for (CheckedExceptionImpl ce : exceptions) {
            if (ce.getExcpetionClass()==exceptionClass)
                return ce;
        }
        return null;
    }

    /**
     * @return a list of checked Exceptions thrown by this method
     */
    public List<CheckedExceptionImpl> getCheckedExceptions(){
        return Collections.unmodifiableList(exceptions);
    }
    /**
     * @param detailType
     * @return Gets the CheckedException corresponding to detailType. Returns
     *         null if no CheckedExcpetion with the detailType found.
     */
    public CheckedExceptionImpl getCheckedException(TypeReference detailType) {
        for (CheckedExceptionImpl ce : exceptions) {
            TypeReference actual = ce.getDetailType();
            if (actual.tagName.equals(detailType.tagName) && actual.type==detailType.type) {
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

    /*package*/ void freeze(WSDLPortImpl portType) {
        this.wsdlOperation = portType.getBinding().get(new QName(portType.getBinding().getPortType().getName().getNamespaceURI(),operationName));
        // TODO: replace this with proper error handling
        if(wsdlOperation ==null)
            throw new Error("Undefined operation name "+operationName);
    }
}

