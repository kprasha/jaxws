package com.sun.xml.ws.wsdl.parser;

import com.sun.xml.ws.api.model.wsdl.WSDLBoundOperation;
import com.sun.xml.ws.api.model.wsdl.WSDLBoundPortType;
import com.sun.xml.ws.api.model.wsdl.WSDLFault;
import com.sun.xml.ws.api.model.wsdl.WSDLInput;
import com.sun.xml.ws.api.model.wsdl.WSDLMessage;
import com.sun.xml.ws.api.wsdl.parser.WSDLParserExtension;
import com.sun.xml.ws.api.model.wsdl.WSDLService;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.model.wsdl.WSDLModel;
import com.sun.xml.ws.api.model.wsdl.WSDLOperation;
import com.sun.xml.ws.api.model.wsdl.WSDLOutput;
import com.sun.xml.ws.api.model.wsdl.WSDLPortType;
import com.sun.xml.ws.streaming.XMLStreamReaderUtil;

import javax.xml.stream.XMLStreamReader;

/**
 * {@link WSDLParserExtension} that delegates to
 * multiple {@link WSDLParserExtension}s.
 *
 * <p>
 * This simplifies {@link RuntimeWSDLParser} since it now
 * only needs to work with one {@link WSDLParserExtension}.
 *
 * <p>
 * This class is guaranteed to return true from
 * all the extension callback methods.
 *
 * @author Kohsuke Kawaguchi
 */
final class WSDLParserExtensionFacade extends WSDLParserExtension {
    private final WSDLParserExtension[] extensions;
    
    WSDLParserExtensionFacade(WSDLParserExtension... extensions) {
        assert extensions!=null;
        this.extensions = extensions;
    }
    
    public boolean serviceElements(WSDLService service, XMLStreamReader reader) {
        for (WSDLParserExtension e : extensions) {
            if(e.serviceElements(service,reader))
                return true;
        }
        XMLStreamReaderUtil.skipElement(reader);
        return true;
    }
    
    public void serviceAttributes(WSDLService service, XMLStreamReader reader) {
        for (WSDLParserExtension e : extensions)
            e.serviceAttributes(service,reader);
    }
    
    public boolean portElements(WSDLPort port, XMLStreamReader reader) {
        for (WSDLParserExtension e : extensions) {
            if(e.portElements(port,reader))
                return true;
        }
        XMLStreamReaderUtil.skipElement(reader);
        return true;
    }
    
    public boolean portTypeOperationInput(WSDLOperation op, XMLStreamReader reader) {
        for (WSDLParserExtension e : extensions)
            e.portTypeOperationInput(op,reader);
        
        return false;
    }
    
    public boolean portTypeOperationOutput(WSDLOperation op, XMLStreamReader reader) {
        for (WSDLParserExtension e : extensions)
            e.portTypeOperationOutput(op,reader);
        
        return false;
    }
    
    public boolean portTypeOperationFault(WSDLOperation op, XMLStreamReader reader) {
        for (WSDLParserExtension e : extensions)
            e.portTypeOperationFault(op,reader);
        
        return false;
    }
    
    public void portAttributes(WSDLPort port, XMLStreamReader reader) {
        for (WSDLParserExtension e : extensions)
            e.portAttributes(port,reader);
    }
    
    public boolean definitionsElements(XMLStreamReader reader){
        for (WSDLParserExtension e : extensions) {
            if (e.definitionsElements(reader)) {
                return true;
            }
        }
        XMLStreamReaderUtil.skipElement(reader);
        return true;
    }
    
    public boolean bindingElements(WSDLBoundPortType binding, XMLStreamReader reader){
        for (WSDLParserExtension e : extensions) {
            if (e.bindingElements(binding, reader)) {
                return true;
            }
        }
        XMLStreamReaderUtil.skipElement(reader);
        return true;
    }
    
    public void bindingAttributes(WSDLBoundPortType binding, XMLStreamReader reader){
        for (WSDLParserExtension e : extensions) {
            e.bindingAttributes(binding, reader);
        }
    }
    
    public boolean portTypeElements(WSDLPortType portType, XMLStreamReader reader) {
        for (WSDLParserExtension e : extensions) {
            if (e.portTypeElements(portType, reader)) {
                return true;
            }
        }
        XMLStreamReaderUtil.skipElement(reader);
        return true;
    }
    
    public void portTypeAttributes(WSDLPortType portType, XMLStreamReader reader) {
        for (WSDLParserExtension e : extensions) {
            e.portTypeAttributes(portType, reader);
        }
    }
    
    public boolean portTypeOperationElements(WSDLOperation operation, XMLStreamReader reader) {
        for (WSDLParserExtension e : extensions) {
            if (e.portTypeOperationElements(operation, reader)) {
                return true;
            }
        }
        XMLStreamReaderUtil.skipElement(reader);
        return true;
    }
    
    public void portTypeOperationAttributes(WSDLOperation operation, XMLStreamReader reader) {
        for (WSDLParserExtension e : extensions) {
            e.portTypeOperationAttributes(operation, reader);
        }
    }
    
    public boolean bindingOperationElements(WSDLBoundOperation operation, XMLStreamReader reader) {
        for (WSDLParserExtension e : extensions) {
            if (e.bindingOperationElements(operation, reader)) {
                return true;
            }
        }
        XMLStreamReaderUtil.skipElement(reader);
        return true;
    }
    
    public void bindingOperationAttributes(WSDLBoundOperation operation, XMLStreamReader reader) {
        for (WSDLParserExtension e : extensions) {
            e.bindingOperationAttributes(operation, reader);
        }
    }
    
    public boolean messageElements(WSDLMessage msg, XMLStreamReader reader) {
        for (WSDLParserExtension e : extensions) {
            if (e.messageElements(msg, reader)) {
                return true;
            }
        }
        XMLStreamReaderUtil.skipElement(reader);
        return true;
    }
    
    public void messageAttributes(WSDLMessage msg, XMLStreamReader reader) {
        for (WSDLParserExtension e : extensions) {
            e.messageAttributes(msg, reader);
        }
    }
    
    public boolean portTypeOperationInputElements(WSDLInput input, XMLStreamReader reader) {
        for (WSDLParserExtension e : extensions) {
            if (e.portTypeOperationInputElements(input, reader)) {
                return true;
            }
        }
        XMLStreamReaderUtil.skipElement(reader);
        return true;
    }
    
    public void portTypeOperationInputAttributes(WSDLInput input, XMLStreamReader reader) {
        for (WSDLParserExtension e : extensions) {
            e.portTypeOperationInputAttributes(input, reader);
        }
    }
    
    public boolean portTypeOperationOutputElements(WSDLOutput output, XMLStreamReader reader) {
        for (WSDLParserExtension e : extensions) {
            if (e.portTypeOperationOutputElements(output, reader)) {
                return true;
            }
        }
        XMLStreamReaderUtil.skipElement(reader);
        return true;
    }
    
    public void portTypeOperationOutputAttributes(WSDLOutput output, XMLStreamReader reader) {
        for (WSDLParserExtension e : extensions) {
            e.portTypeOperationOutputAttributes(output, reader);
        }
    }
    
    public boolean portTypeOperationFaultElements(WSDLFault fault, XMLStreamReader reader) {
        for (WSDLParserExtension e : extensions) {
            if (e.portTypeOperationFaultElements(fault, reader)) {
                return true;
            }
        }
        XMLStreamReaderUtil.skipElement(reader);
        return true;
    }
    
    public void portTypeOperationFaultAttributes(WSDLFault fault, XMLStreamReader reader) {
        for (WSDLParserExtension e : extensions) {
            e.portTypeOperationFaultAttributes(fault, reader);
        }
    }
    
    public boolean bindingOperationInputElements(WSDLBoundOperation operation, XMLStreamReader reader) {
        for (WSDLParserExtension e : extensions) {
            if (e.bindingOperationInputElements(operation, reader)) {
                return true;
            }
        }
        XMLStreamReaderUtil.skipElement(reader);
        return true;
    }
    
    public void bindingOperationInputAttributes(WSDLBoundOperation operation, XMLStreamReader reader) {
        for (WSDLParserExtension e : extensions) {
            e.bindingOperationInputAttributes(operation, reader);
        }
    }
    
    public boolean bindingOperationOutputElements(WSDLBoundOperation operation, XMLStreamReader reader) {
        for (WSDLParserExtension e : extensions) {
            if (e.bindingOperationOutputElements(operation, reader)) {
                return true;
            }
        }
        XMLStreamReaderUtil.skipElement(reader);
        return true;
    }
    
    public void bindingOperationOutputAttributes(WSDLBoundOperation operation, XMLStreamReader reader) {
        for (WSDLParserExtension e : extensions) {
            e.bindingOperationOutputAttributes(operation, reader);
        }
    }
    
    public boolean bindingOperationFaultElements(WSDLBoundOperation operation, XMLStreamReader reader) {
        for (WSDLParserExtension e : extensions) {
            if (e.bindingOperationFaultElements(operation, reader)) {
                return true;
            }
        }
        XMLStreamReaderUtil.skipElement(reader);
        return true;
    }
    
    public void bindingOperationFaultAttributes(WSDLBoundOperation operation, XMLStreamReader reader) {
        for (WSDLParserExtension e : extensions) {
            e.bindingOperationFaultAttributes(operation, reader);
        }
    }
    
    public void finished(WSDLModel model) {
        for (WSDLParserExtension e : extensions) {
            e.finished(model);
        }
    }
}
