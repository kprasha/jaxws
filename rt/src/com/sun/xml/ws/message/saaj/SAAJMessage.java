/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.sun.xml.ws.message.saaj;

import com.sun.istack.NotNull;
import com.sun.istack.XMLStreamException2;
import com.sun.istack.Nullable;
import com.sun.istack.FragmentContentHandler;
import com.sun.xml.bind.api.Bridge;
import com.sun.xml.bind.unmarshaller.DOMScanner;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.message.*;
import com.sun.xml.ws.api.server.Container;
import com.sun.xml.ws.api.server.ContainerResolver;
import com.sun.xml.ws.message.AttachmentUnmarshallerImpl;
import com.sun.xml.ws.spi.db.XMLBridge;
import com.sun.xml.ws.message.DOMWriter;
import com.sun.xml.ws.message.DOMWriterFactory;
import com.sun.xml.ws.streaming.DOMStreamReader;
import com.sun.xml.ws.util.DOMUtil;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Attr;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.LocatorImpl;

import javax.activation.DataHandler;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.soap.AttachmentPart;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.MimeHeader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.WebServiceException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * {@link Message} implementation backed by {@link SOAPMessage}.
 *
 * @author Vivek Pandey
 * @author Rama Pulavarthi
 */
public class SAAJMessage extends Message {
    public static final String BINARY_NODES = "com.sun.xml.ws.message.saaj.binarynodes";

    // flag to switch between representations
    private boolean parsedMessage;
    // flag to check if Message API is exercised;
    private boolean accessedMessage;
    private final SOAPMessage sm;

    private HeaderList headers;
    private List<Element> bodyParts;
    private Element payload;

    private String payloadLocalName;
    private String payloadNamespace;
    private SOAPVersion soapVersion;

    //Collect the attrbutes on the enclosing elements so that the same message can be reproduced without loss of any
    // valuable info
    private NamedNodeMap bodyAttrs, headerAttrs, envelopeAttrs;

    public SAAJMessage(SOAPMessage sm) {
        this.sm = sm;
    }

    /**
     * This constructor is a convenience and called by the {@link #copy}
     *
     * @param headers
     * @param sm
     */
    private SAAJMessage(HeaderList headers, AttachmentSet as, SOAPMessage sm) {
        this.sm = sm;
        this.parse();
        if(headers == null)
            headers = new HeaderList();
        this.headers = headers;
        this.attachmentSet = as;
    }

    private void parse() {
        if (!parsedMessage) {
            try {
                access();
                if (headers == null)
                    headers = new SAAJHeaderList();
                SOAPHeader header = sm.getSOAPHeader();
                if (header != null) {
                    headerAttrs = header.getAttributes();
                    Iterator iter = header.examineAllHeaderElements();
                    while (iter.hasNext()) {
                        headers.add(new SAAJHeader((SOAPHeaderElement) iter.next()));
                    }
                }
                attachmentSet = new ChangeAwareAttachmentSetWrapper(new SAAJAttachmentSet(sm));

                if (headers instanceof ChangeAwareHeaderList)
                	((ChangeAwareHeaderList) headers).reset();
                parsedMessage = true;
            } catch (SOAPException e) {
                throw new WebServiceException(e);
            }
        }
    }
    
    private class SAAJHeaderList extends ChangeAwareHeaderList {

		@Override
		public boolean add(Header header) {
			if (parsedMessage) {
				try {
					if (sm.getSOAPHeader() == null) {
						sm.getSOAPPart().getEnvelope().addHeader();
					}
					header.writeTo(sm);
				} catch (SOAPException se) {
					throw new WebServiceException(se);
				}
				return super.add(header, false);
			}
			return super.add(header);
		}

		@Override
	    protected void addInternal(int index, Header header) {
			if (parsedMessage)
				try {
					header.writeTo(sm);
				} catch (SOAPException se) {
					throw new WebServiceException(se);
				}
	    	super.add(index, header);
	    }
	    
		@Override
	    protected Header removeInternal(int index) {
			Header h = super.removeInternal(index);
			QName q = new QName(h.getNamespaceURI(), h.getLocalPart());
			if (parsedMessage) {
				try {
			        SOAPHeader header = sm.getSOAPHeader();
		            if (header != null) {
		                Iterator iter = header.examineAllHeaderElements();
		                while (iter.hasNext()) {
		                	SOAPHeaderElement she = (SOAPHeaderElement) iter.next();
		                	if (q.equals(she.getElementQName())) {
		                		header.removeChild(she);
		                		break;
		                	}
		                }
		            }
				} catch (SOAPException se) {
					throw new WebServiceException(se);
				}
			}
	    	return h;
	    }
    }

    private void access() {
        if (!accessedMessage) {
            try {
                envelopeAttrs = sm.getSOAPPart().getEnvelope().getAttributes();
                Node body = sm.getSOAPBody();
                bodyAttrs = body.getAttributes();
                soapVersion = SOAPVersion.fromNsUri(body.getNamespaceURI());
                //cature all the body elements
                bodyParts = DOMUtil.getChildElements(body);
                //we treat payload as the first body part
                payload = bodyParts.size() > 0 ? bodyParts.get(0) : null;
                // hope this is correct. Caching the localname and namespace of the payload should be fine
                // but what about if a Handler replaces the payload with something else? Weel, may be it
                // will be error condition anyway
                if (payload != null) {
                    payloadLocalName = payload.getLocalName();
                    payloadNamespace = payload.getNamespaceURI();
                }
                accessedMessage = true;
            } catch (SOAPException e) {
                throw new WebServiceException(e);
            }
        }
    }

    public boolean hasHeaders() {
        parse();
        return headers.size() > 0;
    }

    public @NotNull HeaderList getHeaders() {
        parse();
        return headers;
    }
    /**
     * Gets the attachments of this message
     * (attachments live outside a message.)
     */
    @Override
    public @NotNull AttachmentSet getAttachments() {
        parse();
        return attachmentSet;
    }

    /**
     * Optimization hint for the derived class to check
     * if we may have some attachments.
     */
    @Override
    protected boolean hasAttachments() {
        parse();
        return attachmentSet!=null;
    }
    
    public @Nullable String getPayloadLocalPart() {
        access();
        return payloadLocalName;
    }

    public String getPayloadNamespaceURI() {
        access();
        return payloadNamespace;
    }

    public boolean hasPayload() {
        access();
        return payloadNamespace != null;
    }

    private void addAttributes(Element e, NamedNodeMap attrs) {
        if(attrs == null)
            return;
        String elPrefix = e.getPrefix();
        for(int i=0; i < attrs.getLength();i++) {
            Attr a = (Attr)attrs.item(i);
            //check if attr is ns declaration
            if("xmlns".equals(a.getPrefix()) || a.getLocalName().equals("xmlns")) {
                if(elPrefix == null && a.getLocalName().equals("xmlns")) {
                    // the target element has already default ns declaration, dont' override it
                    continue;
                } else if(elPrefix != null && "xmlns".equals(a.getPrefix()) && elPrefix.equals(a.getLocalName())) {
                    //dont bind the prefix to ns again, its already in the target element.
                    continue;
                }
                e.setAttributeNS(a.getNamespaceURI(),a.getName(),a.getValue());
                continue;
            }
            e.setAttributeNS(a.getNamespaceURI(),a.getName(),a.getValue());
        }
    }

    public Source readEnvelopeAsSource() {
        try {
            if (!parsedMessage) {
                SOAPEnvelope se = sm.getSOAPPart().getEnvelope();
                return new DOMSource(se);

            } else {
				SOAPMessage msg = soapVersion.getMessageFactory().createMessage();
                addAttributes(msg.getSOAPPart().getEnvelope(),envelopeAttrs);

                SOAPBody newBody = msg.getSOAPPart().getEnvelope().getBody();
                addAttributes(newBody, bodyAttrs);
                for (Element part : bodyParts) {
                    Node n = newBody.getOwnerDocument().importNode(part, true);
                    newBody.appendChild(n);
                }
                addAttributes(msg.getSOAPHeader(),headerAttrs);
                Iterator<Name> bodyAttrs = sm.getSOAPBody().getAllAttributes();
                if (bodyAttrs != null) {
                	while(bodyAttrs.hasNext()) {
                		Name name = bodyAttrs.next();
                		newBody.addAttribute(name, sm.getSOAPBody().getAttributeValue(name));
                	}
                }
                for (Header header : headers) {
                    header.writeTo(msg);
                }
                SOAPEnvelope se = msg.getSOAPPart().getEnvelope();
                return new DOMSource(se);
            }
        } catch (SOAPException e) {
            throw new WebServiceException(e);
        }
    } 

    public SOAPMessage readAsSOAPMessage() throws SOAPException {
        if (!parsedMessage) {
            return sm;
        } else {
        	if ((!(headers instanceof ChangeAwareHeaderList) || !((ChangeAwareHeaderList) headers).isChanged())
        			&& (!(attachmentSet instanceof ChangeAwareAttachmentSetWrapper) || !((ChangeAwareAttachmentSetWrapper)attachmentSet).isChanged()))
        		return sm;
        	
            SOAPMessage msg = soapVersion.getMessageFactory().createMessage();
            addAttributes(msg.getSOAPPart().getEnvelope(),envelopeAttrs);
            SOAPBody newBody = msg.getSOAPPart().getEnvelope().getBody();
            addAttributes(newBody, bodyAttrs);
            for (Element part : bodyParts) {
                Node n = newBody.getOwnerDocument().importNode(part, true);
                newBody.appendChild(n);
            }
            addAttributes(msg.getSOAPHeader(),headerAttrs);
            Iterator<Name> bodyAttrs = sm.getSOAPBody().getAllAttributes();
            if (bodyAttrs != null) {
            	while(bodyAttrs.hasNext()) {
            		Name name = bodyAttrs.next();
            		newBody.addAttribute(name, sm.getSOAPBody().getAttributeValue(name));
            	}
            }
            for (Header header : headers) {
              header.writeTo(msg);
            }
            for (Attachment att : getAttachments()) {
              AttachmentPart part = msg.createAttachmentPart();
              part.setDataHandler(att.asDataHandler());
              part.setContentId('<' + att.getContentId() + '>');
              addCustomMimeHeaders(att, part);
              msg.addAttachmentPart(part);
            }
            msg.saveChanges();
            return msg;
        }
    }
   private void addCustomMimeHeaders(Attachment att, AttachmentPart part) {
       
        if (att instanceof AttachmentPartWrapper) {
          AttachmentPart attachmentPartToCopy = ((AttachmentPartWrapper) att).getAttachmentPart();
          Iterator allMimeHeaders = attachmentPartToCopy.getAllMimeHeaders();
          while (allMimeHeaders.hasNext()) {
            Object o = allMimeHeaders.next();
            if (o instanceof MimeHeader) {
              MimeHeader mh = (MimeHeader)o;
              String name = mh.getName();
              String lowerName = name.toLowerCase();
               if (!"content-type".equals(lowerName) && !"content-id".equals(lowerName)) {
                 part.addMimeHeader(name, mh.getValue());
               }
            }
          }


        }
    }
    public Source readPayloadAsSource() {
        access();
        return (payload != null) ? new DOMSource(payload) : null;
    }

    public <T> T readPayloadAsJAXB(Unmarshaller unmarshaller) throws JAXBException {
        access();
        if (payload != null) {
            if(hasAttachments())
                unmarshaller.setAttachmentUnmarshaller(new AttachmentUnmarshallerImpl(getAttachments()));
            return (T) unmarshaller.unmarshal(payload);

        }
        return null;
    }

    /** @deprecated */
    public <T> T readPayloadAsJAXB(Bridge<T> bridge) throws JAXBException {
        access();
        if (payload != null)
            return bridge.unmarshal(payload,hasAttachments()? new AttachmentUnmarshallerImpl(getAttachments()) : null);
        return null;
    }
    public <T> T readPayloadAsJAXB(XMLBridge<T> bridge) throws JAXBException {
        access();
        if (payload != null)
            return bridge.unmarshal(payload,hasAttachments()? new AttachmentUnmarshallerImpl(getAttachments()) : null);
        return null;
    }

    public XMLStreamReader readPayload() throws XMLStreamException {
        access();
        if (payload != null) {
            DOMStreamReader dss = new DOMStreamReader();
            dss.setCurrentNode(payload);
            dss.nextTag();
            assert dss.getEventType() == XMLStreamReader.START_ELEMENT;
            return dss;
        }
        return null;
    }

    public void writePayloadTo(XMLStreamWriter sw) throws XMLStreamException {
        access();
        try {
            Container c = ContainerResolver.getInstance().getContainer();
            DOMWriterFactory dFac = null;
            if (c != null)
            	dFac = c.getSPI(DOMWriterFactory.class);
            if (dFac == null)
            	dFac = DOMWriterFactory.getInstance();
            
            DOMWriter d = dFac.create();
            for (Element part : bodyParts)
                d.writeNode(part, sw);
        } catch (XMLStreamException e) {
            throw new WebServiceException(e);
        }
    }

    public void writeTo(XMLStreamWriter writer) throws XMLStreamException {
        try {
            Container c = ContainerResolver.getInstance().getContainer();
            DOMWriterFactory dFac = null;
            if (c != null)
            	dFac = c.getSPI(DOMWriterFactory.class);
            if (dFac == null)
            	dFac = DOMWriterFactory.getInstance();
            
            DOMWriter d = dFac.create();
            writer.writeStartDocument();
            if (!parsedMessage) {
                d.writeNode(sm.getSOAPPart().getEnvelope(), writer);
            } else {
                SOAPEnvelope env = sm.getSOAPPart().getEnvelope();
                d.writeTagWithAttributes(env, writer);
                if (hasHeaders()) {
                    if(env.getHeader() != null) {
                        DOMUtil.writeTagWithAttributes(env.getHeader(), writer);
                    } else {
                        writer.writeStartElement(env.getPrefix(), "Header", env.getNamespaceURI());
                    }
                    int len = headers.size();
                    for (int i = 0; i < len; i++) {
                        headers.get(i).writeTo(writer);
                    }
                    writer.writeEndElement();
                }

                d.writeNode(sm.getSOAPBody(), writer);
                writer.writeEndElement();
            }
            writer.writeEndDocument();
            writer.flush();
        } catch (SOAPException ex) {
            throw new XMLStreamException2(ex);
            //for now. ask jaxws team what to do.
        }
    }

    public void writeTo(ContentHandler contentHandler, ErrorHandler errorHandler) throws SAXException {
        String soapNsUri = soapVersion.nsUri;
        if (!parsedMessage) {
            DOMScanner ds = new DOMScanner();
            ds.setContentHandler(contentHandler);
            ds.scan(sm.getSOAPPart());
        } else {
            contentHandler.setDocumentLocator(NULL_LOCATOR);
            contentHandler.startDocument();
            contentHandler.startPrefixMapping("S", soapNsUri);
            startPrefixMapping(contentHandler, envelopeAttrs,"S");
            contentHandler.startElement(soapNsUri, "Envelope", "S:Envelope", getAttributes(envelopeAttrs));
            if (hasHeaders()) {
                startPrefixMapping(contentHandler, headerAttrs,"S");
                contentHandler.startElement(soapNsUri, "Header", "S:Header", getAttributes(headerAttrs));
                HeaderList headers = getHeaders();
                int len = headers.size();
                for (int i = 0; i < len; i++) {
                    // shouldn't JDK be smart enough to use array-style indexing for this foreach!?
                    headers.get(i).writeTo(contentHandler, errorHandler);
                }
                endPrefixMapping(contentHandler, headerAttrs,"S");
                contentHandler.endElement(soapNsUri, "Header", "S:Header");

            }
            startPrefixMapping(contentHandler, bodyAttrs,"S");
            // write the body
            contentHandler.startElement(soapNsUri, "Body", "S:Body", getAttributes(bodyAttrs));
            writePayloadTo(contentHandler, errorHandler, true);
            endPrefixMapping(contentHandler, bodyAttrs,"S");
            contentHandler.endElement(soapNsUri, "Body", "S:Body");
            endPrefixMapping(contentHandler, envelopeAttrs,"S");
            contentHandler.endElement(soapNsUri, "Envelope", "S:Envelope");
        }
    }
    /**
     * Gets the Attributes that are not namesapce declarations
     * @param attrs
     * @return
     */
    private AttributesImpl getAttributes(NamedNodeMap attrs) {
        AttributesImpl atts = new AttributesImpl();
        if(attrs == null)
            return EMPTY_ATTS;
        for(int i=0; i < attrs.getLength();i++) {
            Attr a = (Attr)attrs.item(i);
            //check if attr is ns declaration
            if("xmlns".equals(a.getPrefix()) || a.getLocalName().equals("xmlns")) {
              continue;
            }
            atts.addAttribute(fixNull(a.getNamespaceURI()),a.getLocalName(),a.getName(),a.getSchemaTypeInfo().getTypeName(),a.getValue());
        }
        return atts;
    }

    /**
     * Collects the ns declarations and starts the prefix mapping, consequently the associated endPrefixMapping needs to be called.
     * @param contentHandler
     * @param attrs
     * @param excludePrefix , this is to excldue the global prefix mapping "S" used at the start
     * @throws SAXException
     */
    private void startPrefixMapping(ContentHandler contentHandler, NamedNodeMap attrs, String excludePrefix) throws SAXException {
        if(attrs == null)
            return;
        for(int i=0; i < attrs.getLength();i++) {
            Attr a = (Attr)attrs.item(i);
            //check if attr is ns declaration
            if("xmlns".equals(a.getPrefix()) || a.getLocalName().equals("xmlns")) {
                if(!fixNull(a.getPrefix()).equals(excludePrefix)) {
                    contentHandler.startPrefixMapping(fixNull(a.getPrefix()), a.getNamespaceURI());
                }
            }
        }
    }

    private void endPrefixMapping(ContentHandler contentHandler, NamedNodeMap attrs, String excludePrefix) throws SAXException {
        if(attrs == null)
            return;
        for(int i=0; i < attrs.getLength();i++) {
            Attr a = (Attr)attrs.item(i);
            //check if attr is ns declaration
            if("xmlns".equals(a.getPrefix()) || a.getLocalName().equals("xmlns")) {
                if(!fixNull(a.getPrefix()).equals(excludePrefix)) {
                    contentHandler.endPrefixMapping(fixNull(a.getPrefix()));
                }
            }
        }
    }

    private static String fixNull(String s) {
        if(s==null) return "";
        else        return s;
    }
    
    private void writePayloadTo(ContentHandler contentHandler, ErrorHandler errorHandler, boolean fragment) throws SAXException {
        if(fragment)
            contentHandler = new FragmentContentHandler(contentHandler);
        DOMScanner ds = new DOMScanner();
        ds.setContentHandler(contentHandler);
        ds.scan(payload);
    }

    /**
     * Creates a copy of a {@link com.sun.xml.ws.api.message.Message}.
     * <p/>
     * <p/>
     * This method creates a new {@link com.sun.xml.ws.api.message.Message} whose header/payload/attachments/properties
     * are identical to this {@link com.sun.xml.ws.api.message.Message}. Once created, the created {@link com.sun.xml.ws.api.message.Message}
     * and the original {@link com.sun.xml.ws.api.message.Message} behaves independently --- adding header/
     * attachment to one {@link com.sun.xml.ws.api.message.Message} doesn't affect another {@link com.sun.xml.ws.api.message.Message}
     * at all.
     * <p/>
     * <h3>Design Rationale</h3>
     * <p/>
     * Since a {@link com.sun.xml.ws.api.message.Message} body is read-once, sometimes
     * (such as when you do fail-over, or WS-RM) you need to
     * create an idential copy of a {@link com.sun.xml.ws.api.message.Message}.
     * <p/>
     * <p/>
     * The actual copy operation depends on the layout
     * of the data in memory, hence it's best to be done by
     * the {@link com.sun.xml.ws.api.message.Message} implementation itself.
     */
    public Message copy() {
        try {
            if (!parsedMessage) {
                return new SAAJMessage(readAsSOAPMessage());
            } else {
                SOAPMessage msg = soapVersion.getMessageFactory().createMessage();
                SOAPBody newBody = msg.getSOAPPart().getEnvelope().getBody();
                for (Element part : bodyParts) {
                    Node n = newBody.getOwnerDocument().importNode(part, true);
                    newBody.appendChild(n);
                }
                Iterator<Name> bodyAttrs = sm.getSOAPBody().getAllAttributes();
                if (bodyAttrs != null) {
                	while(bodyAttrs.hasNext()) {
                		Name name = bodyAttrs.next();
                		newBody.addAttribute(name, sm.getSOAPBody().getAttributeValue(name));
                	}
                }
                return new SAAJMessage(getHeaders(), getAttachments(), msg);
            }
        } catch (SOAPException e) {
            throw new WebServiceException(e);
        }
    }
    private static final AttributesImpl EMPTY_ATTS = new AttributesImpl();
    private static final LocatorImpl NULL_LOCATOR = new LocatorImpl();

    private static class ChangeAwareHeaderList extends HeaderList {
    	private boolean isChanged = false;
    	
    	public boolean isChanged() {
    		return isChanged;
    	}
    	
    	public void reset() {
    		isChanged = false;
    	}

    	protected boolean add(Header header, boolean updateIsChanged) {
    		if (updateIsChanged)
    			isChanged = true;
    		return super.add(header);
    	}
    	
		@Override
		public boolean add(Header header) {
			isChanged = true;
			return super.add(header);
		}

		@Override
		public Header set(int index, Header element) {
			isChanged = true;
			return super.set(index, element);
		}
    }
    
    private static class ChangeAwareAttachmentSetWrapper implements AttachmentSet {
    	private boolean isChanged = false;
    	private AttachmentSet inner;
    	
    	public ChangeAwareAttachmentSetWrapper(AttachmentSet inner) {
    		this.inner = inner;
    	}
    	
    	public boolean isChanged() {
    		return isChanged;
    	}
    	
    	public void reset() {
    		isChanged = false;
    	}
    	
		@Override
		public void add(Attachment att) {
			isChanged = true;
			inner.add(att);
		}

		@Override
		public Attachment get(String contentId) {
			return inner.get(contentId);
		}

		@Override
		public boolean isEmpty() {
			return inner.isEmpty();
		}

		@Override
		public Iterator<Attachment> iterator() {
			return new Iterator<Attachment>() {
				private Iterator<Attachment> it = inner.iterator();
				
				@Override
				public boolean hasNext() {
					return it.hasNext();
				}

				@Override
				public Attachment next() {
					return it.next();
				}

				@Override
				public void remove() {
					isChanged = true;
					it.remove();
				}
			};
		}
    }
    
    private class SAAJAttachment implements Attachment, AttachmentPartWrapper {

        final AttachmentPart ap;

        public SAAJAttachment(AttachmentPart part) {
            this.ap = part;
        }

        public AttachmentPart getAttachmentPart() {
            return ap;
        }

        /**
         * Content ID of the attachment. Uniquely identifies an attachment.
         */
        public String getContentId() {
            return ap.getContentId();
        }

        /**
         * Gets the MIME content-type of this attachment.
         */
        public String getContentType() {
            return ap.getContentType();
        }

        /**
         * Gets the attachment as an exact-length byte array.
         */
        public byte[] asByteArray() {
            try {
                return ap.getRawContentBytes();
            } catch (SOAPException e) {
                throw new WebServiceException(e);
            }
        }

        /**
         * Gets the attachment as a {@link javax.activation.DataHandler}.
         */
        public DataHandler asDataHandler() {
            try {
                return ap.getDataHandler();
            } catch (SOAPException e) {
                throw new WebServiceException(e);
            }
        }

        /**
         * Gets the attachment as a {@link javax.xml.transform.Source}.
         * Note that there's no guarantee that the attachment is actually an XML.
         */
        public Source asSource() {
            try {
                return new StreamSource(ap.getRawContent());
            } catch (SOAPException e) {
                throw new WebServiceException(e);
            }
        }

        /**
         * Obtains this attachment as an {@link java.io.InputStream}.
         */
        public InputStream asInputStream() {
            try {
                return ap.getRawContent();
            } catch (SOAPException e) {
                throw new WebServiceException(e);
            }
        }

        /**
         * Writes the contents of the attachment into the given stream.
         */
        public void writeTo(OutputStream os) throws IOException {
            os.write(asByteArray());
        }

        /**
         * Writes this attachment to the given {@link javax.xml.soap.SOAPMessage}.
         */
        public void writeTo(SOAPMessage saaj) {
            saaj.addAttachmentPart(ap);
        }

        AttachmentPart asAttachmentPart(){
            return ap;
        }
    }

    /**
     * {@link AttachmentSet} for SAAJ.
     *
     * SAAJ wants '&lt;' and '>' for the content ID, but {@link AttachmentSet}
     * doesn't. S this class also does the conversion between them.
     */
    private class SAAJAttachmentSet implements AttachmentSet {

        private Map<String, Attachment> attMap;
        private Iterator attIter;

        public SAAJAttachmentSet(SOAPMessage sm) {
            attIter = sm.getAttachments();
        }

        /**
         * Gets the attachment by the content ID.
         *
         * @return null
         *         if no such attachment exist.
         */
        public Attachment get(String contentId) {
            // if this is the first time then create the attachment Map
            if (attMap == null) {
                if (!attIter.hasNext())
                    return null;
                attMap = createAttachmentMap();
            }
            if(contentId.charAt(0) != '<'){
                return attMap.get('<'+contentId+'>');
            }
            return attMap.get(contentId);
        }

        public boolean isEmpty() {
            if(attMap!=null)
                return attMap.isEmpty();
            else
                return !attIter.hasNext();
        }

        /**
         * Returns an iterator over a set of elements of type T.
         *
         * @return an Iterator.
         */
        public Iterator<Attachment> iterator() {
            if (attMap == null) {
                attMap = createAttachmentMap();
            }
            return attMap.values().iterator();
        }

        private Map<String, Attachment> createAttachmentMap() {
            HashMap<String, Attachment> map = new HashMap<String, Attachment>();
            while (attIter.hasNext()) {
                AttachmentPart ap = (AttachmentPart) attIter.next();
                map.put(ap.getContentId(), new SAAJAttachment(ap));
            }
            return map;
        }

        public void add(Attachment att) {
            attMap.put('<'+att.getContentId()+'>', att);
        }
    }

}
