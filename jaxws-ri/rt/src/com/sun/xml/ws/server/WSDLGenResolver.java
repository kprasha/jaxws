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

package com.sun.xml.ws.server;

import com.sun.xml.stream.buffer.XMLStreamBuffer;
import com.sun.xml.stream.buffer.XMLStreamBufferResult;
import com.sun.xml.ws.api.server.SDDocument;
import com.sun.xml.ws.api.server.SDDocumentSource;
import com.sun.xml.ws.wsdl.writer.WSDLResolver;

import javax.xml.namespace.QName;
import javax.xml.transform.Result;
import javax.xml.ws.Holder;
import javax.xml.ws.WebServiceException;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author WS Development Team
 */
final class WSDLGenResolver implements WSDLResolver {
    
    private List<SDDocumentImpl> docs;
    private final List<SDDocumentSource> newDocs = new ArrayList<SDDocumentSource>();
    private SDDocumentSource abstractWsdlSource;
    private SDDocumentSource concreteWsdlSource;
    
    private SDDocumentImpl abstractWsdl;
    private SDDocumentImpl concreteWsdl;

    /**
     * targetNS -> documents.
     */
    private final Map<String, List<SDDocumentImpl>> nsMapping = new HashMap<String,List<SDDocumentImpl>>();

    private final QName serviceName;
    private final QName portName;

    public WSDLGenResolver(List<SDDocumentImpl> docs,QName serviceName,QName portName) {
        this.docs = docs;
        this.serviceName = serviceName;
        this.portName = portName;

        for (SDDocumentImpl doc : docs) {
            if(doc.isWSDL()) {
                SDDocument.WSDL wsdl = (SDDocument.WSDL) doc;
                if(wsdl.hasPortType())
                    abstractWsdl = doc;
            }
            if(doc.isSchema()) {
                SDDocument.Schema schema = (SDDocument.Schema) doc;
                List<SDDocumentImpl> sysIds = nsMapping.get(schema.getTargetNamespace());
                if (sysIds == null) {
                    sysIds = new ArrayList<SDDocumentImpl>();
                    nsMapping.put(schema.getTargetNamespace(), sysIds);
                }
                sysIds.add(doc);
            }
        }
    }
    
    /*
    public SDDocumentImpl getConcreteWSDL() {
        return concreteWsdl;
    }
     */
    
    public List<SDDocumentSource> getGeneratedDocs() {
        return newDocs;
    }
    
    /**
     * return null if concrete WSDL need not be generated.
     *
     * TODO: but it's not returning null. What am I missing!? - KK
     */
    public Result getWSDL(String filename) {
        XMLStreamBuffer xsb = new XMLStreamBuffer();
        concreteWsdlSource = SDDocumentSource.create(createURL(filename),xsb);
        newDocs.add(concreteWsdlSource);

        /*
        concreteWsdl=SDDocumentImpl.create(sd,serviceName,portName);

        docs.add(concreteWsdl);
*/
        XMLStreamBufferResult r = new XMLStreamBufferResult(xsb);
        r.setSystemId(filename);
        return r;
    }

    private URL createURL(String filename) {
        try {
            return new URL("file://"+filename);
        } catch (MalformedURLException e) {
            // TODO: I really don't think this is the right way to handle this error,
            // WSDLResolver needs to be documented carefully.
            throw new WebServiceException(e);
        }
    }

    /**
     * Updates filename if the suggested filename need to be changed in
     * wsdl:import
     *
     * return null if abstract WSDL need not be generated
     */
    public Result getAbstractWSDL(Holder<String> filename) {
        if (abstractWsdl != null) {
            filename.value = abstractWsdl.getURL().toString();
            return null;                // Don't generate abstract WSDL
        }

        XMLStreamBuffer xsb = new XMLStreamBuffer();
        abstractWsdlSource = SDDocumentSource.create(createURL(filename.value),xsb);
        newDocs.add(abstractWsdlSource);
/*
        abstractWsdl=SDDocumentImpl.create(sd,serviceName,portName);

        docs.add(abstractWsdl);
 */

        XMLStreamBufferResult r = new XMLStreamBufferResult(xsb);
        r.setSystemId(filename.value);
        return r;
    }

    /*
     * Updates filename if the suggested filename need to be changed in
     * xsd:import
     *
     * return null if schema need not be generated
     */
    // TODO: shouldn't file name be an URL?
    public Result getSchemaOutput(String namespace, Holder<String> filename) {
        List<SDDocumentImpl> schemas = nsMapping.get(namespace);
        if (schemas != null) {
            if (schemas.size() > 1) {
                throw new ServerRtException("server.rt.err",
                    "More than one schema for the target namespace "+namespace);
            }
            filename.value = schemas.get(0).getURL().toExternalForm();
            return null;            // Don't generate schema
        }

        XMLStreamBuffer xsb = new XMLStreamBuffer();
        SDDocumentSource sd = SDDocumentSource.create(createURL(filename.value),xsb);
        /*

        docs.add(SDDocumentImpl.create(sd,serviceName,portName));
         */
        newDocs.add(sd);

        XMLStreamBufferResult r = new XMLStreamBufferResult(xsb);
        r.setSystemId(filename.value);
        return r;
    }
    
    public SDDocumentImpl updateDocs() {
        for (SDDocumentSource doc : newDocs) {
            SDDocumentImpl docImpl = SDDocumentImpl.create(doc,serviceName,portName);
            if (doc == concreteWsdlSource) {
                concreteWsdl = docImpl;
            }
            docs.add(docImpl);
        }
        return concreteWsdl;
    }
    
}
