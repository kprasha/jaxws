package com.sun.xml.ws.sandbox.message.impl.jaxb;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Sniffs the root element name and its attributes.
 *
 * @author Kohsuke Kawaguchi
 */
class RootElementSniffer extends DefaultHandler {
    String nsUri = "##error";
    String localName = "##error";

    public void startElement(String uri, String localName, String qName, Attributes a) throws SAXException {
        this.nsUri = uri;
        this.localName = localName;
        checkAttributes(a);
        // no need to parse any further.
        throw aSAXException;
    }

    protected void checkAttributes(Attributes a) {
    }

    private static final SAXException aSAXException = new SAXException();
}
