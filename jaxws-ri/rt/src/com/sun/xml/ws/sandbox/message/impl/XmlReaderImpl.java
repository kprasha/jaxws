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
package com.sun.xml.ws.sandbox.message.impl;

import com.sun.xml.ws.sandbox.message.HeaderList;
import com.sun.xml.ws.sandbox.message.Message;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.LocatorImpl;
import org.xml.sax.helpers.XMLFilterImpl;

import javax.xml.bind.Marshaller;
import javax.xml.bind.JAXBException;
import javax.xml.transform.sax.SAXSource;

/**
 * @author Kohsuke Kawaguchi
 */
final class XMLReaderImpl extends XMLFilterImpl {

    /**
     * {@link ContentHandler} to fire events to.
     * We use a dummy instance to make sure this will never be null.
     */
    private ContentHandler  contentHandler = DUMMY;

    private final Message msg;

    XMLReaderImpl(Message msg) {
        this.msg = msg;
    }

    public void parse(String systemId) {
        reportError();
    }

    private void reportError() {
        // TODO: i18n
        throw new IllegalStateException(
            "This is a special XMLReader implementation that only works with the InputSource given in SAXSource.");
    }

    public void parse(InputSource input) throws SAXException {
        if(input!=THE_SOURCE)
            reportError();
        msg.writeTo(this,this);
    }

    @Override
    public ContentHandler getContentHandler() {
        if(contentHandler==DUMMY)   return null;
        return contentHandler;
    }

    @Override
    public void setContentHandler(ContentHandler contentHandler) {
        if(contentHandler==null)    contentHandler = DUMMY;
        this.contentHandler = contentHandler;
    }

    private static final ContentHandler DUMMY = new DefaultHandler();

    /**
     * Special {@link InputSource} instance that we use to pass to {@link SAXSource}.
     */
    protected static final InputSource THE_SOURCE = new InputSource();
}
