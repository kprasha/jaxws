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


import com.sun.xml.ws.api.BindingID;
import com.sun.xml.ws.api.addressing.MemberSubmissionEndpointReference;
import com.sun.xml.ws.client.WSServiceDelegate;
import com.sun.xml.ws.transport.http.server.EndpointImpl;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.ws.*;
import javax.xml.ws.spi.Provider;
import javax.xml.ws.spi.ServiceDelegate;
import java.net.URL;

/**
 * @author WS Development Team
 */
public class ProviderImpl extends Provider {

    private final static JAXBContext eprjc = getEPRJaxbContext();

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

    public <T> T getPort(EndpointReference endpointReference, Class<T> serviceEndpointInterface) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public <T> Dispatch<T> createDispatch(EndpointReference endpointReference, Class<T> type, Service.Mode mode) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Dispatch<Object> createDispatch(EndpointReference endpointReference, JAXBContext context, Service.Mode mode) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    private final static JAXBContext getEPRJaxbContext() {
        try {
            return JAXBContext.newInstance(MemberSubmissionEndpointReference.class, W3CEndpointReference.class);
        } catch (JAXBException e) {
            throw new WebServiceException("Error creating JAXBContext for W3CEndpointReference. ", e);
        }
    }
}
