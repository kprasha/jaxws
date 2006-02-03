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
package com.sun.xml.ws.api.wsdl.writer;

import com.sun.xml.txw2.TypedXmlWriter;

import com.sun.xml.ws.wsdl.writer.document.Service;
import com.sun.xml.ws.wsdl.writer.document.Port;
import com.sun.xml.ws.wsdl.writer.document.PortType;
import com.sun.xml.ws.wsdl.writer.document.Binding;
import com.sun.xml.ws.wsdl.writer.document.Operation;
import com.sun.xml.ws.wsdl.writer.document.BindingOperationType;
import com.sun.xml.ws.wsdl.writer.document.Message;
import com.sun.xml.ws.wsdl.writer.document.FaultType;

import java.lang.reflect.Method;

/**
 * This is a callback interface used to extend the WSDLGenerator.  Implementors
 * of this interface can add their own WSDL extensions to the generated WSDL.
 * There are a number of methods that will be invoked allowing the extensions
 * to be generated on various WSDL elements.
 * <p/>
 * The JAX-WS WSDLGenerator uses TXW to serialize the WSDL out to XML.
 * More information about TXW can be located at
 * <a href="http://txw.dev.java.net">http://txw.dev.java.net</a>.
 */
public interface WSDLGeneratorExtension {
    /**
     * This method is invoked so that extensions to a <code>wsdl:service</code>
     * element can be generated.
     * <p/>
     *
     * @param service This is the <code>wsdl:service</code> element that the extension can be added to.
     */
    public void addServiceExtension(Service service);

    /**
     * This method is invoked so that extensions to a <code>wsdl:port</code>
     * element can be generated.
     * <p/>
     *
     * @param port This is the wsdl:port element that the extension can be added to.
     */
    public void addPortExtension(Port port);

    /**
     * This method is invoked so that extensions to a <code>wsdl:portType</code>
     * element can be generated.
     * <p/>
     *
     * @param portType This is the wsdl:portType element that the extension can be added to.
     */
    public void addPortTypeExtension(PortType portType);

    /**
     * This method is invoked so that extensions to a <code>wsdl:binding</code>
     * element can be generated.
     * <p/>
     *
     * TODO:  Some other information may need to be passed
     *
     * @param binding This is the wsdl:binding element that the extension can be added to.
     */
    public void addBindingExtension(Binding binding);

    /**
     * This method is invoked so that extensions to a <code>wsdl:portType/wsdl:operation</code>
     * element can be generated.
     *
     * @param operation This is the wsdl:portType/wsdl:operation  element that the
     *                  extension can be added to.
     * @param method    Annotations from the {@link Method} can be accessed and translated in to WSDL extensibility
     *                  element on wsdl:operation.
     */
    public void addOperationExtension(Operation operation, Method method);


    /**
     * This method is invoked so that extensions to a <code>wsdl:binding/wsdl:operation</code>
     * element can be generated.
     * <p/>
     *
     * @param operation This is the wsdl:binding/wsdl:operation  element that the
     *                  extension can be added to.
     * @param method    {@link java.lang.annotation.Annotation}s from the {@link Method} can be accessed and translated
     *                  in to WSDL extensibility element on wsdl:operation.
     */
    public void addBindingOperationExtension(BindingOperationType operation, Method method);

    /**
     * This method is invoked so that extensions to an input <code>wsdl:message</code>
     * element can be generated.
     * <p/>
     *
     * @param message This is the input wsdl:message element that the
     *                extension can be added to.
     * @param method  Annotations from the {@link Method} can be accessed and translated in to WSDL extensibility
     *                element on wsdl:operation.
     */
    public void addInputMessageExtension(Message message, Method method);

    /**
     * This method is invoked so that extensions to an output <code>wsdl:message</code>
     * element can be generated.
     * <p/>
     *
     * @param message This is the output wsdl:message element that the
     *                extension can be added to.
     * @param method  Annotations from the {@link Method} can be accessed and translated in to WSDL extensibility
     *                element on wsdl:operation.
     */
    public void addOutputMessageExtension(Message message, Method method);


    /**
     * This method is invoked so that extensions to a
     * <code>wsdl:portType/wsdl:operation/wsdl:input</code>
     * element can be generated.
     * <p/>
     *
     * @param input  This is the wsdl:portType/wsdl:operation/wsdl:input  element that the
     *               extension can be added to.
     * @param method Annotations from the {@link Method} can be accessed and translated in to WSDL extensibility
     *               element on wsdl:operation.
     */
    public void addOperationInputExtension(TypedXmlWriter input, Method method);


    /**
     * This method is invoked so that extensions to a
     * <code>wsdl:portType/wsdl:operation/wsdl:output</code>
     * element can be generated.
     * <p/>
     *
     * @param output This is the wsdl:portType/wsdl:operation/wsdl:output  element that the
     *               extension can be added to.
     * @param method Annotations from the {@link Method} can be accessed and translated in to WSDL extensibility
     *               element on wsdl:operation.
     */
    public void addOperationOutputExtension(TypedXmlWriter output, Method method);

    /**
     * This method is invoked so that extensions to a
     * <code>wsdl:binding/wsdl:operation/wsdl:input</code>
     * element can be generated.
     * <p/>
     *
     * @param input  This is the wsdl:binding/wsdl:operation/wsdl:input  element that the
     *               extension can be added to.
     * @param method Annotations from the {@link Method} can be accessed and translated in to WSDL extensibility
     *               element on wsdl:operation.
     */
    public void addBindingOperationInputExtension(TypedXmlWriter input, Method method);


    /**
     * This method is invoked so that extensions to a
     * <code>wsdl:binding/wsdl:operation/wsdl:output</code>
     * element can be generated.
     * <p/>
     *
     * @param output This is the wsdl:binding/wsdl:operation/wsdl:output  element that the
     *               extension can be added to.
     * @param method Annotations from the {@link Method} can be accessed and translated in to WSDL extensibility
     *               element on wsdl:operation.
     */
    public void addBindingOperationOutputExtension(TypedXmlWriter output, Method method);

    /**
     * TODO: Probably it should be removed, apparaently there is no usecase where there is need to read annotations
     * off checked exception class or detail bean that represents wsdl fault.
     * <p/>
     * This method is invoked so that extensions to a
     * <code>wsdl:binding/wsdl:operation/wsdl:fault</code>
     * element can be generated.
     * <p/>
     *
     * @param fault  This is the wsdl:binding/wsdl:operation/wsdl:fault  element that the
     *               extension can be added to.
     * @param method Annotations from the {@link Method} can be accessed and translated in to WSDL extensibility
     *               element on wsdl:operation.
     */
    public void addBindingOperationFaultExtension(FaultType fault, Method method);

    /**
     * TODO: Probably it should be removed, apparaently there is no usecase where there is need to read annotations
     * off checked exception class or detail bean that represents wsdl fault.
     * <p/>
     * This method is invoked so that extensions to a fault <code>wsdl:message</code>
     * element can be generated.
     * <p/>
     *
     * @param message This is the fault wsdl:message element that the
     *                extension can be added to.
     * @param method  Annotations from the {@link Method} can be accessed and translated in to WSDL extensibility
     *                element on wsdl:operation.
     */
    public void addFaultMessageExtension(Message message, Method method);

    /**
     * TODO: Probably it should be removed, apparaently there is no usecase where there is need to read annotations
     * off checked exception class or detail bean that represents wsdl fault.
     * <p/>
     * This method is invoked so that extensions to a
     * <code>wsdl:portType/wsdl:operation/wsdl:fault</code>
     * element can be generated.
     * <p/>
     *
     * @param fault  This is the wsdl:portType/wsdl:operation/wsdl:fault  element that the
     *               extension can be added to.
     * @param method Annotations from the {@link Method} can be accessed and translated in to WSDL extensibility
     *               element on wsdl:operation.
     */
    public void addOperationFaultExtension(FaultType fault, Method method);

}
