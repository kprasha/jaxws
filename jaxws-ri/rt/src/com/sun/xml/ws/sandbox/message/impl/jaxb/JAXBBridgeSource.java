package com.sun.xml.ws.sandbox.message.impl.jaxb;

import com.sun.xml.bind.api.Bridge;
import com.sun.xml.bind.api.BridgeContext;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.XMLFilterImpl;

import javax.xml.bind.JAXBException;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import java.io.IOException;

/**
 * Wraps a bridge and JAXB object into a pseudo-{@link Source}.
 * @author Kohsuke Kawaguchi
 */
final class JAXBBridgeSource extends SAXSource {

    public JAXBBridgeSource( Bridge bridge, BridgeContext context, Object contentObject ) {
        this.bridge = bridge;
        this.context = context;
        this.contentObject = contentObject;

        super.setXMLReader(pseudoParser);
        // pass a dummy InputSource. We don't care
        super.setInputSource(new InputSource());
    }

    private final Bridge bridge;
    private final BridgeContext context;
    private final Object contentObject;

    // this object will pretend as an XMLReader.
    // no matter what parameter is specified to the parse method,
    // it just parse the contentObject.
    private final XMLReader pseudoParser = new XMLFilterImpl() {
        public boolean getFeature(String name) throws SAXNotRecognizedException {
            if(name.equals("http://xml.org/sax/features/namespaces"))
                return true;
            if(name.equals("http://xml.org/sax/features/namespace-prefixes"))
                return false;
            throw new SAXNotRecognizedException(name);
        }

        public void setFeature(String name, boolean value) throws SAXNotRecognizedException {
            if(name.equals("http://xml.org/sax/features/namespaces") && value)
                return;
            if(name.equals("http://xml.org/sax/features/namespace-prefixes") && !value)
                return;
            throw new SAXNotRecognizedException(name);
        }

        public Object getProperty(String name) throws SAXNotRecognizedException {
            if( "http://xml.org/sax/properties/lexical-handler".equals(name) ) {
                return lexicalHandler;
            }
            throw new SAXNotRecognizedException(name);
        }

        public void setProperty(String name, Object value) throws SAXNotRecognizedException {
            if( "http://xml.org/sax/properties/lexical-handler".equals(name) ) {
                this.lexicalHandler = (LexicalHandler)value;
                return;
            }
            throw new SAXNotRecognizedException(name);
        }

        private LexicalHandler lexicalHandler;

        public void parse(InputSource input) throws IOException, SAXException {
            parse();
        }

        public void parse(String systemId) throws IOException, SAXException {
            parse();
        }

        public void parse() throws SAXException {
            // parses a content object by using the given bridge
            // SAX events will be sent to the repeater, and the repeater
            // will further forward it to an appropriate component.
            try {
                bridge.marshal( context, contentObject, this );
            } catch( JAXBException e ) {
                // wrap it to a SAXException
                SAXParseException se =
                    new SAXParseException( e.getMessage(),
                        null, null, -1, -1, e );

                // if the consumer sets an error handler, it is our responsibility
                // to notify it.
                fatalError(se);

                // this is a fatal error. Even if the error handler
                // returns, we will abort anyway.
                throw se;
            }
        }
    };
}
