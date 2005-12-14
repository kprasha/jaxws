package com.sun.xml.ws.sandbox.message.impl.saaj;

import com.sun.xml.ws.sandbox.message.Header;
import com.sun.xml.ws.sandbox.XMLStreamWriterEx;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.JAXBException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPException;

/**
 * $Id: SAAJHeader.java,v 1.1.2.1 2005-12-14 01:46:54 vivekp Exp $
 */

/**
 * Copyright 2005 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
public class SAAJHeader implements Header{
    /**
     * True if this header must be understood.
     */
    public boolean isMustUnderstood() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Gets the value of the soap:role attribute (or soap:actor for SOAP 1.1).
     * <p/>
     * <p/>
     * SOAP 1.1 values are normalized into SOAP 1.2 values.
     * <p/>
     * An omitted SOAP 1.1 actor attribute value will become:
     * "http://www.w3.org/2003/05/soap-envelope/role/ultimateReceiver"
     * An SOAP 1.1 actor attribute value of:
     * "http://schemas.xmlsoap.org/soap/actor/next"
     * will become:
     * "http://www.w3.org/2003/05/soap-envelope/role/next"
     * <p/>
     * <p/>
     * If the soap:role attribute is absent, this method returns
     * "http://www.w3.org/2003/05/soap-envelope/role/ultimateReceiver".
     *
     * @return never null. This string need not be interned.
     */
    public String getRole() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * True if this header is to be relayed if not processed.
     * For SOAP 1.1 messages, this method always return false.
     * <p/>
     * <p/>
     * IOW, this method returns true if there's @soap:relay='true'
     * is present.
     * <p/>
     * <h3>Implementation Note</h3>
     * <p/>
     * The implementation needs to check for both "true" and "1",
     * but because attribute values are normalized, it doesn't have
     * to consider " true", " 1 ", and so on.
     *
     * @return false.
     */
    public boolean isRelay() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Gets the namespace URI of this header element.
     *
     * @return never null.
     *         this string must be interned.
     */
    public String getNamespaceURI() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Gets the local name of this header element.
     *
     * @return never null.
     *         this string must be interned.
     */
    public String getLocalPart() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Reads the header as a {@link javax.xml.stream.XMLStreamReader}.
     * <p/>
     * <p/>
     * <h3>Performance Expectation</h3>
     * <p/>
     * For some {@link com.sun.xml.ws.sandbox.message.Header} implementations, this operation
     * is a non-trivial operation. Therefore, use of this method
     * is discouraged unless the caller is interested in reading
     * the whole header.
     * <p/>
     * <p/>
     * Similarly, if the caller wants to use this method only to do
     * the API conversion (such as simply firing SAX events from
     * {@link javax.xml.stream.XMLStreamReader}), then the JAX-WS team requests
     * that you talk to us.
     *
     * @return must not null.
     */
    public XMLStreamReader readHeader() throws XMLStreamException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Reads the header as a JAXB object by using the given unmarshaller.
     */
    public <T> T readAsJAXB(Unmarshaller unmarshaller) throws JAXBException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Writes out the header.
     *
     * @throws javax.xml.stream.XMLStreamException
     *          if the operation fails for some reason. This leaves the
     *          writer to an undefined state.
     */
    public void writeTo(XMLStreamWriterEx w) throws XMLStreamException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Writes out the header to the given SOAPMessage.
     * <p/>
     * TODO: justify why this is necessary
     *
     * @throws javax.xml.soap.SOAPException if the operation fails for some reason. This leaves the
     *                                      writer to an undefined state.
     */
    public void writeTo(SOAPMessage saaj) throws SOAPException {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
