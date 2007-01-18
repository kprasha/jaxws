/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License).  You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the license at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.xml.ws.encoding.fastinfoset;

import java.io.InputStream;
import java.io.Reader;
import javax.xml.stream.EventFilter;
import javax.xml.stream.StreamFilter;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLReporter;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.util.XMLEventAllocator;
import javax.xml.transform.Source;

/**
 * @author Alexey Stashok
 */
public class FastInfosetXMLInputFactory extends XMLInputFactory {
    public boolean isPropertySupported(String string) {
        return false;
    }
    
    public XMLReporter getXMLReporter() {
        return null;
    }
    
    public void setXMLReporter(XMLReporter xMLReporter) {
    }
    
    public XMLResolver getXMLResolver() {
        return null;
    }
    
    public void setXMLResolver(XMLResolver xMLResolver) {
    }
    
    public XMLEventAllocator getEventAllocator() {
        return null;
    }
    
    public void setEventAllocator(XMLEventAllocator xMLEventAllocator) {
    }
    
    public Object getProperty(String string) throws IllegalArgumentException {
        return null;
    }
    
    public void setProperty(String string, Object object) throws IllegalArgumentException {
    }
    
    public XMLEventReader createXMLEventReader(InputStream inputStream) throws XMLStreamException {
        return null;
    }
    
    public XMLEventReader createXMLEventReader(Reader reader) throws XMLStreamException {
        return null;
    }
    
    public XMLEventReader createXMLEventReader(XMLStreamReader xMLStreamReader) throws XMLStreamException {
        return null;
    }
    
    public XMLEventReader createXMLEventReader(Source source) throws XMLStreamException {
        return null;
    }
    
    public XMLStreamReader createXMLStreamReader(InputStream inputStream) throws XMLStreamException {
        return null;
    }
    
    public XMLStreamReader createXMLStreamReader(Reader reader) throws XMLStreamException {
        return null;
    }
    
    public XMLStreamReader createXMLStreamReader(Source source) throws XMLStreamException {
        return null;
    }
    
    public XMLEventReader createXMLEventReader(String string, InputStream inputStream) throws XMLStreamException {
        return null;
    }
    
    public XMLEventReader createXMLEventReader(String string, Reader reader) throws XMLStreamException {
        return null;
    }
    
    public XMLEventReader createXMLEventReader(InputStream inputStream, String string) throws XMLStreamException {
        return null;
    }
    
    public XMLEventReader createFilteredReader(XMLEventReader xMLEventReader, EventFilter eventFilter) throws XMLStreamException {
        return null;
    }
    
    public XMLStreamReader createXMLStreamReader(String string, InputStream inputStream) throws XMLStreamException {
        return FastInfosetCodec.createNewStreamReader(inputStream, false);
    }
    
    public XMLStreamReader createXMLStreamReader(String string, Reader reader) throws XMLStreamException {
        return null;
    }
    
    public XMLStreamReader createXMLStreamReader(InputStream inputStream, String string) throws XMLStreamException {
        return null;
    }
    
    public XMLStreamReader createFilteredReader(XMLStreamReader xMLStreamReader, StreamFilter streamFilter) throws XMLStreamException {
        return null;
    }
}
