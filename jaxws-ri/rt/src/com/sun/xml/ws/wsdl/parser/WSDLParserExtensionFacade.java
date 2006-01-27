package com.sun.xml.ws.wsdl.parser;

import com.sun.xml.ws.api.wsdl.parser.WSDLParserExtension;
import com.sun.xml.ws.api.model.wsdl.WSDLService;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.model.wsdl.WSDLModel;
import com.sun.xml.ws.streaming.XMLStreamReaderUtil;

import javax.xml.stream.XMLStreamReader;
import java.util.Collection;
import java.util.Arrays;

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

    public boolean service(WSDLService service, XMLStreamReader reader) {
        for (WSDLParserExtension e : extensions) {
            if(e.service(service,reader))
                return true;
        }
        XMLStreamReaderUtil.skipElement(reader);
        return true;
    }

    public boolean port(WSDLPort port, XMLStreamReader reader) {
        for (WSDLParserExtension e : extensions) {
            if(e.port(port,reader))
                return true;
        }
        XMLStreamReaderUtil.skipElement(reader);
        return true;
    }

    public void finished(WSDLModel model) {
        for (WSDLParserExtension e : extensions) {
            e.finished(model);
        }
    }
}
