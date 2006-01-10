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
package com.sun.xml.ws.sandbox.api.wsdl.writer;

import com.sun.xml.txw2.TypedXmlWriter;

import com.sun.xml.ws.wsdl.writer.document.*;

/**
 * This is a callback interface used to extend the WSDLGenerator.  Implementors
 * of this interface can add their own WSDL extensions to the generated WSDL.
 * There are a number of methods that will be invoked allowing the extensions 
 * to be generated on various WSDL elements.
 * 
 * The JAX-WS WSDLGenerator uses TXW to serialize the WSDL out to XML.
 * More information about TXW can be located at 
 * <a href="http://txw.dev.java.net">http://txw.dev.java.net</a>.
 */
public interface WSDLGeneratorExtension {
    /**
     * This method is invoked so that extensions to a <code>wsdl:service</code>
     * element can be generated.
     * 
     * TODO:  Some other information may need to be passed
     * @param service This is the <code>wsdl:service</code> element that the extension can be added to.
     */
    public void addServiceExtension(Service service);

    /**
     * This method is invoked so that extensions to a <code>wsdl:port</code>
     * element can be generated.
     * 
     * TODO:  Some other information may need to be passed
     * @param port This is the wsdl:port element that the extension can be added to.
     */
    public void addPortExtension(com.sun.xml.ws.wsdl.writer.document.Port port);
    
    /**
     * This method is invoked so that extensions to a <code>wsdl:portType</code>
     * element can be generated.
     * 
     * TODO:  Some other information may need to be passed
     * @param portType This is the wsdl:portType element that the extension can be added to.
     */
    public void addPortTypeExtension(com.sun.xml.ws.wsdl.writer.document.PortType portType);
    
    /**
     * This method is invoked so that extensions to a <code>wsdl:binding</code>
     * element can be generated.
     * 
     * TODO:  Some other information may need to be passed
     * @param binding This is the wsdl:binding element that the extension can be added to.
     */
    public void addBindingExtension(com.sun.xml.ws.wsdl.writer.document.Binding binding);

    /**
     * This method is invoked so that extensions to a <code>wsdl:portType/wsdl:operation</code>
     * element can be generated.
     * 
     * TODO:  Some other information may need to be passed
     *        Also, the JavaMethod class will be changed to reflect the unmutable 
     *        RuntimeModel apis
     * @param operation This is the wsdl:portType/wsdl:operation  element that the 
     *        extension can be added to.
     * @param javaMethod This is the JaveMethod represented by <code>operation</code> 
     */
    public void addOperationExtension(com.sun.xml.ws.wsdl.writer.document.Operation operation, 
            com.sun.xml.ws.sandbox.api.model.JavaMethod javaMethod);
    

    /**
     * This method is invoked so that extensions to a <code>wsdl:binding/wsdl:operation</code>
     * element can be generated.
     * 
     * TODO:  Some other information may need to be passed
     *        Also, the JavaMethod class will be changed to reflect the unmutable 
     *        RuntimeModel apis
     * @param operation This is the wsdl:binding/wsdl:operation  element that the 
     *        extension can be added to.
     * @param javaMethod This is the JaveMethod represented by <code>operation</code> 
     */
    public void addBindingOperationExtension(com.sun.xml.ws.wsdl.writer.document.BindingOperationType operation, 
            com.sun.xml.ws.sandbox.api.model.JavaMethod javaMethod);
    
    /**
     * This method is invoked so that extensions to an input <code>wsdl:message</code>
     * element can be generated.
     * 
     * TODO:  Some other information may need to be passed
     *        Also, the JavaMethod class will be changed to reflect the unmutable 
     *        RuntimeModel apis
     * @param message This is the input wsdl:message element that the 
     *        extension can be added to.
     * @param javaMethod This is the JaveMethod used to generate <code>message</code> 
     */
    public void addInputMessageExtension(com.sun.xml.ws.wsdl.writer.document.Message message, 
            com.sun.xml.ws.sandbox.api.model.JavaMethod javaMethod);

    /**
     * This method is invoked so that extensions to an output <code>wsdl:message</code>
     * element can be generated.
     * 
     * TODO:  Some other information may need to be passed
     *        Also, the JavaMethod class will be changed to reflect the unmutable 
     *        RuntimeModel apis
     * @param message This is the output wsdl:message element that the 
     *        extension can be added to.
     * @param javaMethod This is the JaveMethod used to generate output <code>message</code> 
     */
    public void addOutputMessageExtension(com.sun.xml.ws.wsdl.writer.document.Message message, 
            com.sun.xml.ws.sandbox.api.model.JavaMethod javaMethod);

    
    /**
     * This method is invoked so that extensions to a fault <code>wsdl:message</code>
     * element can be generated.
     * 
     * TODO:  Some other information may need to be passed
     *        Also, the JavaMethod class will be changed to reflect the unmutable 
     *        RuntimeModel apis
     * @param message This is the fault wsdl:message element that the 
     *        extension can be added to.
     * @param javaMethod This is the JaveMethod that throws the <code>ce</code> 
     * @param ce This is the CheckedException corresponding to this fault <code>message</code>
     */
    public void addFaultMessageExtension(com.sun.xml.ws.wsdl.writer.document.Message message, 
            com.sun.xml.ws.sandbox.api.model.JavaMethod javaMethod, com.sun.xml.ws.sandbox.api.model.CheckedException ce);
    
    
    /**
     * This method is invoked so that extensions to a 
     * <code>wsdl:portType/wsdl:operation/wsdl:input</code>
     * element can be generated.
     * 
     * TODO:  Some other information may need to be passed
     *        Also, the JavaMethod class will be changed to reflect the unmutable 
     *        RuntimeModel apis
     * @param input This is the wsdl:portType/wsdl:operation/wsdl:input  element that the 
     *        extension can be added to.
     * @param javaMethod This is the JaveMethod represented by <code>wsdl:operation/wsdl:input</code> 
     */
    public void addOperationInputExtension(TypedXmlWriter input, 
            com.sun.xml.ws.sandbox.api.model.JavaMethod javaMethod);


    /**
     * This method is invoked so that extensions to a 
     * <code>wsdl:portType/wsdl:operation/wsdl:output</code>
     * element can be generated.
     * 
     * TODO:  Some other information may need to be passed
     *        Also, the JavaMethod class will be changed to reflect the unmutable 
     *        RuntimeModel apis
     * @param output This is the wsdl:portType/wsdl:operation/wsdl:output  element that the 
     *        extension can be added to.
     * @param javaMethod This is the JaveMethod represented by <code>wsdl:operation/wsdl:output</code> 
     */
    public void addOperationOutputExtension(TypedXmlWriter output, 
            com.sun.xml.ws.sandbox.api.model.JavaMethod javaMethod);

    /**
     * This method is invoked so that extensions to a 
     * <code>wsdl:portType/wsdl:operation/wsdl:fault</code>
     * element can be generated.
     * 
     * TODO:  Some other information may need to be passed
     *        Also, the JavaMethod class will be changed to reflect the unmutable 
     *        RuntimeModel apis
     * @param fault This is the wsdl:portType/wsdl:operation/wsdl:fault  element that the 
     *        extension can be added to.
     * @param javaMethod This is the JaveMethod represented by <code>wsdl:operation/wsdl:fault</code> 
     * @param ce This is the CheckedException corresponding to the <code>fault</code>
     */
    public void addOperationFaultExtension(com.sun.xml.ws.wsdl.writer.document.FaultType fault, 
            com.sun.xml.ws.sandbox.api.model.JavaMethod javaMethod, com.sun.xml.ws.sandbox.api.model.CheckedException ce);



    /**
     * This method is invoked so that extensions to a 
     * <code>wsdl:binding/wsdl:operation/wsdl:input</code>
     * element can be generated.
     * 
     * TODO:  Some other information may need to be passed
     *        Also, the JavaMethod class will be changed to reflect the unmutable 
     *        RuntimeModel apis
     * @param input This is the wsdl:binding/wsdl:operation/wsdl:input  element that the 
     *        extension can be added to.
     * @param javaMethod This is the JaveMethod represented by <code>wsdl:operation/wsdl:input</code> 
     */
    public void addBindingOperationInputExtension(TypedXmlWriter input, 
            com.sun.xml.ws.sandbox.api.model.JavaMethod javaMethod);


    /**
     * This method is invoked so that extensions to a 
     * <code>wsdl:binding/wsdl:operation/wsdl:output</code>
     * element can be generated.
     * 
     * TODO:  Some other information may need to be passed
     *        Also, the JavaMethod class will be changed to reflect the unmutable 
     *        RuntimeModel apis
     * @param output This is the wsdl:binding/wsdl:operation/wsdl:output  element that the 
     *        extension can be added to.
     * @param javaMethod This is the JaveMethod represented by <code>wsdl:operation/wsdl:output</code> 
     */
    public void addBindingOperationOutputExtension(TypedXmlWriter output, 
            com.sun.xml.ws.sandbox.api.model.JavaMethod javaMethod);

    /**
     * This method is invoked so that extensions to a 
     * <code>wsdl:binding/wsdl:operation/wsdl:fault</code>
     * element can be generated.
     * 
     * TODO:  Some other information may need to be passed
     *        Also, the JavaMethod class will be changed to reflect the unmutable 
     *        RuntimeModel apis
     * @param fault This is the wsdl:binding/wsdl:operation/wsdl:fault  element that the 
     *        extension can be added to.
     * @param javaMethod This is the JaveMethod represented by <code>wsdl:operation/wsdl:fault</code> 
     * @param ce This is the CheckedException corresponding to the <code>fault</code>
     */
    public void addBindingOperationFaultExtension(com.sun.xml.ws.wsdl.writer.document.FaultType fault, 
            com.sun.xml.ws.sandbox.api.model.JavaMethod javaMethod, com.sun.xml.ws.sandbox.api.model.CheckedException ce);
}
