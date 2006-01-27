package com.sun.xml.ws.api.wsdl.parser;

import com.sun.xml.ws.api.model.wsdl.WSDLService;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.model.wsdl.WSDLModel;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.wsdl.parser.RuntimeWSDLParser;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamConstants;

/**
 * Extends the WSDL parsing process.
 *
 * <p>
 * This interface is implemented by components that build on top of the JAX-WS RI,
 * to participate in the WSDL parsing process that happens in the runtime.
 * This allows such components to retrieve information from WSDL extension elements,
 * and use that later to, for example, configure {@link Pipe}s.
 *
 * <h2>How it works?</h2>
 * <p>
 * Each method on this interface denotes one extension point in WSDL
 * (the place where foreign elements can be added.) A {@link RuntimeWSDLParser}
 * starts parsing WSDL with a fixed set of {@link WSDLParserExtension}s, and
 * as it finds extension elements, it calls appropriate callback methods
 * to provide a chance for {@link WSDLParserExtension} to parse such
 * an extension element.
 *
 * <p>
 * When a callback is invoked, {@link XMLStreamReader} points to the
 * start tag of the extension element. The callback method can do
 * one of the following:
 *
 * <ol>
 *  <li>Return {@code false} without moving {@link XMLStreamReader},
 *      to indicate that the extension element isn't recognized.
 *      This allows the next {@link WSDLParserExtension} to see this
 *      extension element.
 *  <li>Parse the whole subtree rooted at the element,
 *      move the cursor to the {@link XMLStreamConstants#END_ELEMENT} state,
 *      and return {@code true}, indicating that the extension
 *      element is consumed.
 *      No other {@link WSDLParserExtension}s are notified of this extension.
 * </ol>
 *
 * <p>
 * For each callback, the corresponding WSDL model object is passed in,
 * so that {@link WSDLParserExtension} can relate what it's parsing
 * to the {@link WSDLModel}. Note that since the {@link WSDLModel} itself
 * is being built, {@link WSDLParserExtension} may not invoke any of
 * the methods on it. Those references are passed just so that
 * {@link WSDLParserExtension} can hold on to those references,
 * not to query WSDL models.
 *
 * <p>
 * If {@link WSDLParserExtension} needs to query {@link WSDLModel},
 * defer that processing until {@link #finished(WSDLModel)}, when it's
 * safe to use {@link WSDLModel} can be used safely.
 *
 * <p>
 * Also note that {@link WSDLParserExtension}s are called in no particular order.
 * This interface is not designed for having multiple {@link WSDLParserExtension}s
 * parse the same extension element.
 *
 * <h2>TODO</h2>
 * <p>
 * How {@link RuntimeWSDLParser} finds {@link WSDLParserExtension}s
 * need to be determined. It needs to be done in such a way that
 * {@link WSDLParserExtension} can carry the parsed state and use
 * it when pipeline is constructed.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class WSDLParserExtension {
    public abstract boolean service(WSDLService service, XMLStreamReader reader);
    public abstract boolean port(WSDLPort port, XMLStreamReader reader);

    // TODO: define more methods

    /**
     * Called when the parsing of a set of WSDL documents are all done.
     * <p>
     * This is the opportunity to do any post-processing of the parsing
     * you've done.
     *
     * @param model
     *      The completely parsed {@link WSDLModel}. All the methods on
     *      the model can be safely invoked, and expected to work.
     */
    public abstract void finished(WSDLModel model);
}
