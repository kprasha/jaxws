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
package com.sun.xml.ws.spi;


import com.sun.istack.NotNull;
import com.sun.xml.ws.addressing.EndpointReferenceUtil;
import com.sun.xml.ws.api.BindingID;
import com.sun.xml.ws.api.WSService;
import com.sun.xml.ws.api.addressing.AddressingVersion;
import com.sun.xml.ws.api.addressing.WSEndpointReference;
import com.sun.xml.ws.client.WSServiceDelegate;
import com.sun.xml.ws.developer.MemberSubmissionEndpointReference;
import com.sun.xml.ws.transport.http.server.EndpointImpl;
import com.sun.xml.ws.resources.ProviderApiMessages;
import org.w3c.dom.Element;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.ws.Endpoint;
import javax.xml.ws.EndpointReference;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.spi.Provider;
import javax.xml.ws.spi.ServiceDelegate;
import javax.xml.ws.wsaddressing.W3CEndpointReference;
import java.net.URL;
import java.util.List;

/**
 * The entry point to the JAX-WS RI from the JAX-WS API.
 *
 * @author WS Development Team
 */
public class ProviderImpl extends Provider {

    private final static JAXBContext eprjc = getEPRJaxbContext();

    /**
     * Convenient singleton instance.
     */
    public static final ProviderImpl INSTANCE = new ProviderImpl();

    @Override
    public Endpoint createEndpoint(String bindingId, Object implementor) {
        return new EndpointImpl(
            (bindingId == null) ? BindingID.SOAP11_HTTP : BindingID.parse(bindingId),
            implementor);
    }

    @Override
    public ServiceDelegate createServiceDelegate( URL wsdlDocumentLocation, QName serviceName, Class serviceClass) {
         return new WSServiceDelegate(wsdlDocumentLocation, serviceName, serviceClass);
    }

    @Override
    public Endpoint createAndPublishEndpoint(String address,
                                             Object implementor) {
        Endpoint endpoint = new EndpointImpl(
            BindingID.parse(implementor.getClass()),
            implementor);
        endpoint.publish(address);
        return endpoint;
    }

    public Endpoint createEndpoint(String bindingId, String[] features, Object implementor) {
        return null;
    }

    public EndpointReference readEndpointReference(Source eprInfoset) {
        Unmarshaller unmarshaller;
        try {
            unmarshaller = eprjc.createUnmarshaller();
            return (EndpointReference) unmarshaller.unmarshal(eprInfoset);
        } catch (JAXBException e) {
            throw new WebServiceException("Error creating Marshaller or marshalling.", e);
        }
    }

    public <T extends EndpointReference> T createEndpointReference(Class<T> clazz, QName serviceName, QName portName, Source wsdlDocumentLocation, Element... referenceParameters) {
        throw new UnsupportedOperationException("To be done");
    }

    public <T> T getPort(EndpointReference endpointReference, Class<T> clazz, WebServiceFeature... webServiceFeatures) {
        final @NotNull MemberSubmissionEndpointReference msepr =
                EndpointReferenceUtil.transform(MemberSubmissionEndpointReference.class, endpointReference);
        WSService service = new WSServiceDelegate(msepr.toWSDLSource(), msepr.serviceName.name, Service.class);
        return service.getPort(msepr, clazz, webServiceFeatures);
    }

    
    public W3CEndpointReference createW3CEndpointReference(String address, QName serviceName, QName portName, List<Element> metadata, String wsdlDocumentLocation, List<Element> referenceParameters) {
        if (address == null) {
            if (serviceName == null || portName == null) {
                throw new IllegalStateException(ProviderApiMessages.NULL_ADDRESS_SERVICE_ENDPOINT());
            } else {
                //TODO create SPI to get if from JavaEE Container
                //address = getMeAddress(serviceName,portName);

                //address is still null? may be its not run in a JavaEE Container
                if(address == null)
                    throw new IllegalStateException(ProviderApiMessages.NULL_ADDRESS());
            }
        }
        if((serviceName==null) && (portName != null)) {
            throw new IllegalStateException(ProviderApiMessages.NULL_SERVICE());
        }
        return new WSEndpointReference(
            AddressingVersion.fromSpecClass(W3CEndpointReference.class),
            address, serviceName, portName, null, metadata, wsdlDocumentLocation, referenceParameters).toSpec(W3CEndpointReference.class);
    }

    private static JAXBContext getEPRJaxbContext() {
        try {
            return JAXBContext.newInstance(MemberSubmissionEndpointReference.class, W3CEndpointReference.class);
        } catch (JAXBException e) {
            throw new WebServiceException("Error creating JAXBContext for W3CEndpointReference. ", e);
        }
    }
}
