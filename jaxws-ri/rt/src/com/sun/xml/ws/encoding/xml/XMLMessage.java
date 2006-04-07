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

import com.sun.xml.bind.api.Bridge;
import com.sun.xml.bind.api.BridgeContext;
import com.sun.xml.messaging.saaj.packaging.mime.MessagingException;
import com.sun.xml.messaging.saaj.packaging.mime.internet.ContentType;
import com.sun.xml.messaging.saaj.packaging.mime.internet.InternetHeaders;
import com.sun.xml.messaging.saaj.packaging.mime.internet.MimeBodyPart;
import com.sun.xml.messaging.saaj.packaging.mime.internet.MimeMultipart;
import com.sun.xml.messaging.saaj.util.ByteInputStream;
import com.sun.xml.messaging.saaj.util.ByteOutputStream;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.message.HeaderList;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Messages;
import com.sun.xml.ws.encoding.jaxb.JAXBTypeSerializer;
import com.sun.xml.ws.protocol.xml.XMLMessageException;
import com.sun.xml.ws.sandbox.message.impl.AbstractMessageImpl;
import com.sun.xml.ws.streaming.SourceReaderFactory;
import com.sun.xml.ws.streaming.XMLStreamWriterFactory;
import com.sun.xml.ws.util.ByteArrayBuffer;
import com.sun.xml.ws.util.FastInfosetReflection;
import com.sun.xml.ws.util.FastInfosetUtil;
import com.sun.xml.ws.util.xml.XMLStreamReaderToXMLStreamWriter;
import com.sun.xml.ws.util.xml.XmlUtil;
import java.io.ByteArrayOutputStream;
import java.lang.UnsupportedOperationException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.activation.DataHandler;

import javax.activation.DataSource;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.http.HTTPException;
import java.io.ByteArrayInputStream;
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
    private static Message create(final String ct, final InputStream in) {
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
    

    public static Message create(Source source, Map<String, DataHandler> attachments) {
        if (attachments == null) {
            return create(source);
        } else {
            if (source == null) {
                return new UnknownContent(attachments);
            } else {
                return new XMLMultiPart(source, attachments);
            }
        }
    }
    
    public static Message create(Object object, JAXBContext context, Map<String, DataHandler> attachments) {
        if (attachments == null) {
            return (object == null) ? Messages.createEmpty(SOAPVersion.SOAP_11) : create(object, context);
        } else {
            if (object == null) {
                return new UnknownContent(attachments);
            } else {
                return new XMLMultiPart(JAXBTypeSerializer.serialize(object, context), attachments);
            }
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
        private DataSource dataSource;
        private MimeMultipart multipart;
        private final MimeHeaders headers = new MimeHeaders();

        public XMLMultiPart(final String contentType, final InputStream is) {
            super(SOAPVersion.SOAP_11);
            dataSource = new DataSource() {
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
        
        public XMLMultiPart(Source source, final Map<String, DataHandler> atts) {
            super(SOAPVersion.SOAP_11);
            multipart = new MimeMultipart("related");
            multipart.getContentType().setParameter("type", "text/xml");

            // Creates Primary part
            MimeBodyPart rootPart = new MimeBodyPart();
            rootPart.setContent(source, "text/xml");
            multipart.addBodyPart(rootPart, 0);

            for(Map.Entry<String, DataHandler> e : atts.entrySet()) {
                MimeBodyPart part = new MimeBodyPart();
                part.setDataHandler(e.getValue());
                multipart.addBodyPart(part);
            }
        }

        public XMLMultiPart(DataSource dataSource) {
            super(SOAPVersion.SOAP_11);
            this.dataSource = dataSource;
        }

        public DataSource getDataSource() {
            if (dataSource != null) {
                return dataSource;
            } else if (multipart != null) {
                return new DataSource() {
                    public InputStream getInputStream() {
                        try {
                            ByteOutputStream bos = new ByteOutputStream();
                            multipart.writeTo(bos);
                            return bos.newInputStream();
                        } catch(MessagingException me) {
                            throw new XMLMessageException("xml.get.ds.err",me);
                        } catch(IOException ioe) {
                            throw new XMLMessageException("xml.get.ds.err",ioe);
                        }
                    }

                    public OutputStream getOutputStream() {
                        return null;
                    }

                    public String getContentType() {
                        return multipart.getContentType().toString();
                    }

                    public String getName() {
                        return "";
                    }
                };
            }
            return null;
        }

        private MimeBodyPart getRootPart() {
            try {
                convertToMultipart();
                ContentType contentType = multipart.getContentType();
                String startParam = contentType.getParameter("start");
                MimeBodyPart sourcePart = (startParam == null)
                    ? (MimeBodyPart)multipart.getBodyPart(0)
                    : (MimeBodyPart)multipart.getBodyPart(startParam);
                return sourcePart;
            }
            catch (MessagingException ex) {
                throw new XMLMessageException("xml.get.source.err",ex);
            }
        }

        private void convertToMultipart() {
            if (dataSource != null) {
                try {
                    multipart = new MimeMultipart(dataSource,null);
                    dataSource = null;
                } catch (MessagingException ex) {
                    throw new XMLMessageException("xml.get.source.err",ex);
                }
            }
        }

        public void writeTo(OutputStream out, boolean useFastInfoset) {
            try {
                // Try to use dataSource whenever possible
                if (dataSource != null) {
                    InputStream is = dataSource.getInputStream();
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = is.read(buf)) != -1) {
                        out.write(buf, 0, len);
                    }
                    return;     // we're done
                }     
                multipart.writeTo(out);
            }
            catch(Exception e) {
                throw new WebServiceException(e);
            }
        }
        
        MimeHeaders getMimeHeaders() {
            headers.removeHeader("Content-Type");
            if (dataSource != null) {
                headers.addHeader("Content-Type", dataSource.getContentType());
            } else {
                if (multipart != null ) {
                    headers.addHeader("Content-Type", multipart.getContentType().toString());
                }
            }
            return headers;
        }

        protected void writePayloadTo(ContentHandler contentHandler, ErrorHandler errorHandler) throws SAXException {
            throw new UnsupportedOperationException();
        }

        public boolean hasHeaders() {
            throw new UnsupportedOperationException();
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
            // Otherwise, parse MIME package and find root part
            convertToMultipart();
            MimeBodyPart sourcePart = getRootPart();
            try {
                // Return a StreamSource or FastInfosetSource depending on type
                return new StreamSource(sourcePart.getInputStream());
            } catch (IOException ex) {
                throw new WebServiceException(ex);
            }
        }

        public XMLStreamReader readPayload() throws XMLStreamException {
            return SourceReaderFactory.createSourceReader(readPayloadAsSource(), true);
        }

        public void writePayloadTo(XMLStreamWriter sw) throws XMLStreamException {
            new XMLStreamReaderToXMLStreamWriter().bridge(readPayload(), sw);
        }

        public Message copy() {
            throw new UnsupportedOperationException();
        }

    }

    
    /**
     * Don't know about this content. It's conent-type is NOT the XML types
     * we recognize(text/xml, application/xml, multipart/related;text/xml etc).
     *
     * This could be used to represent image/jpeg etc
     */
    public static class UnknownContent extends AbstractMessageImpl implements HasDataSource {
        private final String ct;
        private final InputStream in;
        private final MimeMultipart multipart;
        private final MimeHeaders headers = new MimeHeaders();
        
        public UnknownContent(String ct, InputStream in) {
            super(SOAPVersion.SOAP_11);
            this.ct = ct;
            this.in = in;
            this.multipart = null;
        }
        
        public UnknownContent(Map<String, DataHandler> atts) {
            super(SOAPVersion.SOAP_11);
            this.in = null;
            multipart = new MimeMultipart("mixed");
            for(Map.Entry<String, DataHandler> e : atts.entrySet()) {
                MimeBodyPart part = new MimeBodyPart();
                part.setDataHandler(e.getValue());
                multipart.addBodyPart(part);
            }
            this.ct = multipart.getContentType().toString();
        }

        public DataSource getDataSource() {
            return new DataSource() {
                public InputStream getInputStream() {
                    if (multipart != null) {
                        try {
                            ByteOutputStream bos = new ByteOutputStream();
                            multipart.writeTo(bos);
                            return bos.newInputStream();
                        } catch(Exception ioe) {
                            throw new WebServiceException(ioe);
                        }
                    }
                    return in;
                }

                public OutputStream getOutputStream() {
                    return null;
                }

                public String getContentType() {
                    assert ct != null;
                    return ct;
                }

                public String getName() {
                    return "";
                }
            };
        }

        MimeHeaders getMimeHeaders() {
            headers.removeHeader("Content-Type");
            headers.addHeader("Content-Type", ct);
            return headers;
        }

        protected void writePayloadTo(ContentHandler contentHandler, ErrorHandler errorHandler) throws SAXException {
            throw new UnsupportedOperationException();
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
            return false;
        }

        public Source readPayloadAsSource() {
            return null;
        }

        public XMLStreamReader readPayload() throws XMLStreamException {
            throw new WebServiceException("There is not XML payload. Shouldn't come here.");
        }

        public void writePayloadTo(XMLStreamWriter sw) throws XMLStreamException {
            // No XML. Nothing to do
        }

        public Message copy() {
            throw new UnsupportedOperationException();
        }

    }
    
    public static interface HasDataSource {
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
    
}
