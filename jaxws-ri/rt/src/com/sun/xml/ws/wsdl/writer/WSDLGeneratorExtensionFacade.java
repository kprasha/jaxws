package com.sun.xml.ws.wsdl.writer;

import com.sun.xml.ws.api.wsdl.writer.WSDLGeneratorExtension;

import com.sun.xml.txw2.TypedXmlWriter;

import com.sun.xml.ws.api.model.CheckedException;
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
 * {@link WSDLGeneratExtension} that delegates to
 * multiple {@link WSDLGeneratorExtension}s.
 *
 * <p>
 * This simplifies {@link WSDLGenerator} since it now
 * only needs to work with one {@link WSDLGeneratorExtension}.
 *
 *
 * @author Doug Kohlert
 */
final class WSDLGeneratorExtensionFacade implements WSDLGeneratorExtension {
    private final WSDLGeneratorExtension[] extensions;

    WSDLGeneratorExtensionFacade(WSDLGeneratorExtension... extensions) {
        assert extensions!=null;
        this.extensions = extensions;
    }

    public void addServiceExtension(Service service) {
        for (WSDLGeneratorExtension e : extensions) 
            e.addServiceExtension(service);
    }

    public void addPortExtension(Port port) {
        for (WSDLGeneratorExtension e : extensions) 
            e.addPortExtension(port);
    }

    public void addPortTypeExtension(PortType portType) {
        for (WSDLGeneratorExtension e : extensions) 
            e.addPortTypeExtension(portType);
    }

    public void addBindingExtension(Binding binding) {
        for (WSDLGeneratorExtension e : extensions) 
            e.addBindingExtension(binding);
    }

    public void addOperationExtension(Operation operation, Method method) {
        for (WSDLGeneratorExtension e : extensions) 
            e.addOperationExtension(operation, method);
    }

    public void addBindingOperationExtension(BindingOperationType operation, Method method) {
        for (WSDLGeneratorExtension e : extensions) 
            e.addBindingOperationExtension(operation, method);
    }
    
    public void addInputMessageExtension(Message message, Method method) {
        for (WSDLGeneratorExtension e : extensions) 
            e.addInputMessageExtension(message, method);
    }

    public void addOutputMessageExtension(Message message, Method method) {
        for (WSDLGeneratorExtension e : extensions) 
            e.addOutputMessageExtension(message, method);
    }

    public void addOperationInputExtension(TypedXmlWriter input, Method method) {
        for (WSDLGeneratorExtension e : extensions) 
            e.addOperationInputExtension(input, method);
    }

    public void addOperationOutputExtension(TypedXmlWriter output, Method method) {
        for (WSDLGeneratorExtension e : extensions) 
            e.addOperationOutputExtension(output, method);
    }

    public void addBindingOperationInputExtension(TypedXmlWriter input, Method method) {
        for (WSDLGeneratorExtension e : extensions) 
            e.addBindingOperationInputExtension(input, method);
    }
    
    public void addBindingOperationOutputExtension(TypedXmlWriter output, Method method) {
        for (WSDLGeneratorExtension e : extensions) 
            e.addBindingOperationOutputExtension(output, method);
    }

    public void addBindingOperationFaultExtension(FaultType fault, Method method, CheckedException ce) {
        for (WSDLGeneratorExtension e : extensions) 
            e.addBindingOperationFaultExtension(fault, method, ce);
    }

    public void addFaultMessageExtension(Message message, Method method, CheckedException ce) {
        for (WSDLGeneratorExtension e : extensions) 
            e.addFaultMessageExtension(message, method, ce);
    }

    public void addOperationFaultExtension(FaultType fault, Method method, CheckedException ce) {
        for (WSDLGeneratorExtension e : extensions) 
            e.addOperationFaultExtension(fault, method, ce);
    }
}
