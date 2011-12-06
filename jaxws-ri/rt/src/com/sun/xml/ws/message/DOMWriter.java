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

package com.sun.xml.ws.message;

import java.util.Iterator;

import javax.activation.DataHandler;
import javax.xml.XMLConstants;
import javax.xml.bind.DatatypeConverter;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jvnet.staxex.XMLStreamWriterEx;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.istack.NotNull;
import com.sun.istack.Nullable;
import com.sun.xml.ws.util.ByteArrayDataSource;

public class DOMWriter {
	private static final DOMWriter instance = new DOMWriter();
	
	public static DOMWriter getInstance() {
		return instance;
	}
	
	private boolean isSuppressRepeatedEmptyNS;
	
	public DOMWriter(boolean isSuppressRepeatedEmptyNS) {
		this.isSuppressRepeatedEmptyNS = isSuppressRepeatedEmptyNS;
	}
	
	public DOMWriter() {
		this(false);
	}
	
    private static
    @NotNull
    String fixNull(@Nullable String s) {
        if (s == null) return "";
        else return s;
    }

    private static boolean isPrefixDeclared(XMLStreamWriter writer, String nsUri, String prefix) {
        boolean prefixDecl = false;
        NamespaceContext nscontext = writer.getNamespaceContext();
        Iterator prefixItr = nscontext.getPrefixes(nsUri);
        while (prefixItr.hasNext()) {
            if (prefix.equals(prefixItr.next())) {
                prefixDecl = true;
                break;
            }
        }
        return prefixDecl;
    }

    public void writeTagWithAttributes(Element node, XMLStreamWriter writer) throws XMLStreamException {
    	writeTagWithAttributes(node, writer, false);
    }
    
    protected boolean writeTagWithAttributes(Element node, XMLStreamWriter writer, boolean isEmptyNSSeen) throws XMLStreamException {
        String nodePrefix = fixNull(node.getPrefix());
        String nodeNS = fixNull(node.getNamespaceURI());
        //fix to work with DOM level 1 nodes.
        String nodeLocalName = node.getLocalName()== null?node.getNodeName():node.getLocalName();
        
        // See if nodePrefix:nodeNS is declared in writer's NamespaceContext before writing start element
        // Writing start element puts nodeNS in NamespaceContext even though namespace declaration not written
        boolean prefixDecl = isPrefixDeclared(writer, nodeNS, nodePrefix);
        writer.writeStartElement(nodePrefix, nodeLocalName, nodeNS);

        if (node.hasAttributes()) {
            NamedNodeMap attrs = node.getAttributes();
            int numOfAttributes = attrs.getLength();
            // write namespace declarations first.
            // if we interleave this with attribue writing,
            // Zephyr will try to fix it and we end up getting inconsistent namespace bindings.
            for (int i = 0; i < numOfAttributes; i++) {
                Node attr = attrs.item(i);
                String nsUri = fixNull(attr.getNamespaceURI());
                if (nsUri.equals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI)) {
                    // handle default ns declarations
                    String local = attr.getLocalName().equals(XMLConstants.XMLNS_ATTRIBUTE) ? "" : attr.getLocalName();
                    if (local.equals(nodePrefix) && attr.getNodeValue().equals(nodeNS)) {
                        prefixDecl = true;
                    }
                    if (local.equals("")) {
                    	boolean isEmptyNS = attr.getNodeValue() == null || "".equals(attr.getNodeValue());
                    	if (!isEmptyNS || !isSuppressRepeatedEmptyNS || !isEmptyNSSeen) {
                    		writer.writeDefaultNamespace(attr.getNodeValue());
                    		if (isEmptyNS)
                    			isEmptyNSSeen = true;
                    	}
                    } else {
                        // this is a namespace declaration, not an attribute
                        writer.setPrefix(attr.getLocalName(), attr.getNodeValue());
                        writer.writeNamespace(attr.getLocalName(), attr.getNodeValue());
                    }
                }
            }
        }
        // node's namespace is not declared as attribute, but declared on ancestor
        if (!prefixDecl) {
        	boolean isEmptyNS = (nodeNS == null || "".equals(nodeNS)) && (nodePrefix == null || "".equals(nodePrefix) || XMLConstants.XMLNS_ATTRIBUTE.equals(nodePrefix));
        	if (!isEmptyNS || !isSuppressRepeatedEmptyNS || !isEmptyNSSeen) {
        		writer.writeNamespace(nodePrefix, nodeNS);
        		if (isEmptyNS)
        			isEmptyNSSeen = true;
        	}
        }

        // Write all other attributes which are not namespace decl.
        if (node.hasAttributes()) {
            NamedNodeMap attrs = node.getAttributes();
            int numOfAttributes = attrs.getLength();

            for (int i = 0; i < numOfAttributes; i++) {
                Node attr = attrs.item(i);
                String attrPrefix = fixNull(attr.getPrefix());
                String attrNS = fixNull(attr.getNamespaceURI());
                if (!attrNS.equals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI)) {
                    String localName = attr.getLocalName();
                    if (localName == null) {
                        // TODO: this is really a bug in the caller for not creating proper DOM tree.
                        // will remove this workaround after plugfest
                        localName = attr.getNodeName();
                    }
                    boolean attrPrefixDecl = isPrefixDeclared(writer, attrNS, attrPrefix);
                    if (!attrPrefix.equals("") && !attrPrefixDecl) {
                        // attr has namespace but namespace decl is there in ancestor node
                        // So write the namespace decl before writing the attr
                        writer.setPrefix(attr.getLocalName(), attr.getNodeValue());
                        writer.writeNamespace(attrPrefix, attrNS);
                    }
                    
                    writer.writeAttribute(attrPrefix, attrNS, localName, attr.getNodeValue());
                }
            }
        }
        return isEmptyNSSeen;
    }

    /**
     * Traverses a DOM node and writes out on a streaming writer.
     *
     * @param node
     * @param writer
     */
	public void writeNode(Element node, XMLStreamWriter writer) throws XMLStreamException {
		writeNode(node, writer, false);
	}

	protected boolean writeNode(Element node, XMLStreamWriter writer, boolean isEmptyNSSeen) throws XMLStreamException {
		isEmptyNSSeen = writeTagWithAttributes(node, writer, isEmptyNSSeen);

        if (node.hasChildNodes()) {
            NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                switch (child.getNodeType()) {
                    case Node.PROCESSING_INSTRUCTION_NODE:
                        writer.writeProcessingInstruction(child.getNodeValue());
                    case Node.DOCUMENT_TYPE_NODE:
                        break;
                    case Node.CDATA_SECTION_NODE:
                        writer.writeCData(child.getNodeValue());
                        break;
                    case Node.COMMENT_NODE:
                        writer.writeComment(child.getNodeValue());
                        break;
                    case Node.TEXT_NODE:
                    	writeTextNode(child, writer);
                        break;
                    case Node.ELEMENT_NODE:
                    	isEmptyNSSeen = writeNode((Element) child, writer, isEmptyNSSeen);
                        break;
                }
            }
        }
        writer.writeEndElement();
        return isEmptyNSSeen;
	}
	
	protected void writeTextNode(Node child, XMLStreamWriter writer) throws XMLStreamException {
   		writer.writeCharacters(child.getNodeValue());
	}
}
