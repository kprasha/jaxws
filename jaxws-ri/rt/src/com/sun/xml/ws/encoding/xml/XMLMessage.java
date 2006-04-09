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

package com.sun.xml.ws.encoding.xml;

import com.sun.xml.messaging.saaj.packaging.mime.internet.ContentType;
import com.sun.xml.messaging.saaj.packaging.mime.internet.MimeMultipart;
import com.sun.xml.messaging.saaj.util.ByteOutputStream;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.message.HeaderList;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Messages;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.sandbox.impl.XMLHTTPDecoder;
import com.sun.xml.ws.sandbox.message.impl.AbstractMessageImpl;
import com.sun.xml.ws.streaming.XMLStreamWriterFactory;

import javax.activation.DataSource;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.soap.MimeHeaders;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.WebServiceException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

/**
 *
 * @author Jitendra Kotamraju
 */
public final class XMLMessage {

    private static final Logger log = Logger.getLogger(
        com.sun.xml.ws.util.Constants.LoggingDomain + ".protocol.xml");
    
    // So that SAAJ registers DCHs for MIME types
    static {
        new com.sun.xml.messaging.saaj.soap.AttachmentPartImpl();
    }

    private static final int PLAIN_XML_FLAG      = 1;       // 00001
    private static final int MIME_MULTIPART_FLAG = 2;       // 00010
    private static final int FI_ENCODED_FLAG     = 16;      // 10000


    /**
     * Construct a message from an input stream. When messages are
     * received, there's two parts -- the transport headers and the
     * message content in a transport specific stream.
     */
    public static Message create(final String ct, final InputStream in) {
        Message data;
        try {
            if (ct != null) {
                ContentType contentType = new ContentType(ct);
                int contentTypeId = identifyContentType(contentType);
                boolean isFastInfoset = (contentTypeId & FI_ENCODED_FLAG) > 0;
                if ((contentTypeId & MIME_MULTIPART_FLAG) != 0) {
                    data = new XMLMultiPart(ct, in);
                } else if ((contentTypeId & PLAIN_XML_FLAG) != 0 || (contentTypeId & FI_ENCODED_FLAG) != 0) {
                    data = Messages.createUsingPayload(new StreamSource(in), SOAPVersion.SOAP_11);
                } else {
                    data = new UnknownContent(ct, in);
                }
            } else {
                data = Messages.createEmpty(SOAPVersion.SOAP_11);
            }
        } catch(Exception ex) {
            throw new WebServiceException(ex);
        }
        return data;
    }


    public static Message create(Source source) {
        return (source == null) ? Messages.createEmpty(SOAPVersion.SOAP_11) : Messages.createUsingPayload(source, SOAPVersion.SOAP_11);
    }

    public static Message create(DataSource ds) {
        try {
            return (ds == null) ? Messages.createEmpty(SOAPVersion.SOAP_11) : create(ds.getContentType(), ds.getInputStream());
        } catch(IOException ioe) {
            throw new WebServiceException(ioe);
        }
    }

    public static Message create(Object object, JAXBContext context) {
        if (object == null) {
            return Messages.createEmpty(SOAPVersion.SOAP_11);
        } else {
            Marshaller marshaller;
            try {
                marshaller = context.createMarshaller();
            } catch (JAXBException ex) {
                throw new WebServiceException(ex);
            }
            return Messages.create(marshaller,  object, SOAPVersion.SOAP_11);
        }
    }


    /**
     * Verify a contentType.
     *
     * @return
     * MIME_MULTIPART_FLAG | PLAIN_XML_FLAG
     * MIME_MULTIPART_FLAG | FI_ENCODED_FLAG;
     * PLAIN_XML_FLAG
     * FI_ENCODED_FLAG
     *
     */
    private static int identifyContentType(ContentType contentType) {
        String primary = contentType.getPrimaryType();
        String sub = contentType.getSubType();

        if (primary.equalsIgnoreCase("multipart") && sub.equalsIgnoreCase("related")) {
            String type = contentType.getParameter("type");
            if (type != null) {
                if (isXMLType(type)) {
                    return MIME_MULTIPART_FLAG | PLAIN_XML_FLAG;
                } else if (isFastInfosetType(type)) {
                    return MIME_MULTIPART_FLAG | FI_ENCODED_FLAG;
                }
            }
            return 0;
        } else if (isXMLType(primary, sub)) {
            return PLAIN_XML_FLAG;
        } else if (isFastInfosetType(primary, sub)) {
            return FI_ENCODED_FLAG;
        }
        return 0;
    }

    protected static boolean isXMLType(String primary, String sub) {
        return (primary.equalsIgnoreCase("text") || primary.equalsIgnoreCase("application")) && sub.equalsIgnoreCase("xml");
    }

    protected static boolean isXMLType(String type) {
        return type.toLowerCase().startsWith("text/xml") ||
            type.toLowerCase().startsWith("application/xml");
    }

    protected static boolean isFastInfosetType(String primary, String sub) {
        return primary.equalsIgnoreCase("application") && sub.equalsIgnoreCase("fastinfoset");
    }

    protected static boolean isFastInfosetType(String type) {
        return type.toLowerCase().startsWith("application/fastinfoset");
    }

    private static String getContentType(MimeHeaders headers) {
        String[] values = headers.getHeader("Content-Type");
        return (values == null) ? null : values[0];
    }
    
    /**
     * Data represented as a multi-part MIME message. It also has XML as
     * root part
     *
     * This class parses {@link MimeMultipart} lazily.
     */
    private static final class XMLMultiPart extends AbstractMessageImpl implements HasDataSource {
        private final DataSource dataSource;
        private Message message = null;

        public XMLMultiPart(final String contentType, final InputStream is) {
            super(SOAPVersion.SOAP_11);
            dataSource = createDataSource(contentType, is);
        }

        public XMLMultiPart(DataSource dataSource) {
            super(SOAPVersion.SOAP_11);
            this.dataSource = dataSource;
        }

        public DataSource getDataSource() {
            assert dataSource != null;
            return dataSource;
        }
        
        private void convertDataSourceToMessage() {
            if (message != null) {
                try {
                    message = XMLHTTPDecoder.INSTANCE.decodeXMLMultipart(dataSource.getInputStream(), dataSource.getContentType());
                } catch(IOException ioe) {
                    throw new WebServiceException(ioe);
                }
            }
        }

        protected void writePayloadTo(ContentHandler contentHandler, ErrorHandler errorHandler) throws SAXException {
            throw new UnsupportedOperationException();
        }
        
        public boolean isFault() {
            return false;
        }

        public boolean hasHeaders() {
            return false;
        }

        public HeaderList getHeaders() {
            throw new UnsupportedOperationException();
        }

        public String getPayloadLocalPart() {
            throw new UnsupportedOperationException();
        }

        public String getPayloadNamespaceURI() {
            throw new UnsupportedOperationException();
        }

        public boolean hasPayload() {
            return true;
        }

        public Source readPayloadAsSource() {
            convertDataSourceToMessage();
            return message.readPayloadAsSource();
        }

        public XMLStreamReader readPayload() throws XMLStreamException {
            convertDataSourceToMessage();
            return message.readPayload();
        }

        public void writePayloadTo(XMLStreamWriter sw) throws XMLStreamException {
            convertDataSourceToMessage();
            message.writePayloadTo(sw);
        }

        public Message copy() {
            convertDataSourceToMessage();
            return message.copy();
        }

        public boolean hasUnconsumedDataSource() {
            return message == null;
        }

    }

    
    /**
     * Don't know about this content. It's conent-type is NOT the XML types
     * we recognize(text/xml, application/xml, multipart/related;text/xml etc).
     *
     * This could be used to represent image/jpeg etc
     */
    public static class UnknownContent extends AbstractMessageImpl implements HasDataSource {
        private final DataSource ds;
        
        public UnknownContent(final String ct, final InputStream in) {
            super(SOAPVersion.SOAP_11);
            ds = createDataSource(ct, in);
        }
        
        public UnknownContent(DataSource ds) {
            super(SOAPVersion.SOAP_11);
            this.ds = ds;
        }
        
        public boolean hasUnconsumedDataSource() {
            return true;
        }

        public DataSource getDataSource() {
            assert ds != null;
            return ds;
        }

        protected void writePayloadTo(ContentHandler contentHandler, ErrorHandler errorHandler) throws SAXException {
            throw new UnsupportedOperationException();
        }

        public boolean hasHeaders() {
            return false;
        }
        
        public boolean isFault() {
            return false;
        }

        public HeaderList getHeaders() {
            throw new UnsupportedOperationException();
        }

        public String getPayloadLocalPart() {
            throw new UnsupportedOperationException();
        }

        public String getPayloadNamespaceURI() {
            throw new UnsupportedOperationException();
        }

        public boolean hasPayload() {
            return false;
        }

        public Source readPayloadAsSource() {
            return null;
        }

        public XMLStreamReader readPayload() throws XMLStreamException {
            throw new WebServiceException("There isn't XML payload. Shouldn't come here.");
        }

        public void writePayloadTo(XMLStreamWriter sw) throws XMLStreamException {
            // No XML. Nothing to do
        }

        public Message copy() {
            throw new UnsupportedOperationException();
        }

    }
    
    public static interface HasDataSource {
        boolean hasUnconsumedDataSource();
        DataSource getDataSource();
    }
    
    public static DataSource getDataSource(Message msg) {
        if (msg instanceof HasDataSource) {
            return ((HasDataSource)msg).getDataSource();
        } else {
            final ByteOutputStream bos = new ByteOutputStream();
            XMLStreamWriter writer = XMLStreamWriterFactory.createXMLStreamWriter(bos);
            try {
                msg.writePayloadTo(writer);
                writer.flush();
            } catch (XMLStreamException e) {
                throw new WebServiceException(e);
            }
            return new DataSource() {
                public InputStream getInputStream() {
                    return bos.newInputStream();
                }

                public OutputStream getOutputStream() {
                    return null;
                }

                public String getContentType() {
                    return "text/xml";
                }

                public String getName() {
                    return "";
                }
            };
        }
        
    }
    
    public static DataSource createDataSource(final String contentType, final InputStream is) {
        return new DataSource() {
            public InputStream getInputStream() {
                return is;
            }

            public OutputStream getOutputStream() {
                return null;
            }

            public String getContentType() {
                return contentType;
            }

            public String getName() {
                return "";
            }
        };
    }
    
}
