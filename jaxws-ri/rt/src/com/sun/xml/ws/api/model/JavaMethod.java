/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License).  You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the license at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * 
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.xml.ws.api.model;

import com.sun.xml.ws.api.model.soap.SOAPBinding;
import com.sun.xml.ws.api.model.wsdl.WSDLBoundOperation;

import javax.jws.WebParam.Mode;
import java.lang.reflect.Method;

/**
 * Abstracts the annotated {@link Method} of a SEI.
 *
 * @author Vivek Pandey
 */
public interface JavaMethod {

    /**
     * Gets the root {@link SEIModel} that owns this model.
     */
    SEIModel getOwner();

    /**
     * @return Returns the java {@link Method}
     */
    Method getMethod();

    /**
     * @return Returns the {@link MEP}.
     */
    MEP getMEP();

    /**
     * Binding object - a {@link SOAPBinding} isntance.
     *
     * @return the Binding object
     */
    SOAPBinding getBinding();

    /**
     * The {@link WSDLBoundOperation} that this method represents.
     *
     * @return
     *      always non-null.
     */
//    WSDLBoundOperation getOperation();

    /**
     * Request parameters can be {@link Mode#IN} or
     * {@link Mode#INOUT} and these parameters go in a request message on-the-wire.
     * Further a Parameter can be instance of {@link com.sun.xml.ws.model.WrapperParameter} when
     * the operation is wrapper style.
     *
     * @return returns unmodifiable list of request parameters
     */
//    List<? extends Parameter> getRequestParameters();

    /**
     * Response parameters go in the response message on-the-wire and can be of
     * {@link Mode#OUT} or {@link Mode#INOUT}
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
