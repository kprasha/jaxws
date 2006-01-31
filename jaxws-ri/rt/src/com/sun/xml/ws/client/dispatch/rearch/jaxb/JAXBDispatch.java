/*
 * Copyright (c) 2006 Sun Microsystems Corporation. All Rights Reserved.
 */

package com.sun.xml.ws.client.dispatch.rearch.jaxb;

import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.MessageProperties;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.binding.BindingImpl;
import static com.sun.xml.ws.client.BindingProviderProperties.JAXB_CONTEXT_PROPERTY;
import com.sun.xml.ws.client.WSServiceDelegate;
import com.sun.xml.ws.client.dispatch.rearch.DispatchImpl;
import com.sun.xml.ws.sandbox.message.impl.jaxb.JAXBMessage;
import com.sun.xml.ws.util.Pool;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO: Use sandbox classes, update javadoc
 */

/**
 * The <code>JAXBDispatch</code> class provides support
 * for the dynamic invocation of a service endpoint operation using
 * JAXB objects. The <code>javax.xml.ws.Service</code>
 * interface acts as a factory for the creation of <code>JAXBDispatch</code>
 * instances.
 *
 * @author WS Development Team
 * @version 1.0
 */

public class JAXBDispatch extends DispatchImpl<Object> {

    private final JAXBContext jaxbcontext;

    private final Pool.Marshaller marshallers;
    private final Pool.Unmarshaller unmarshallers;


    public JAXBDispatch(QName port, JAXBContext jc, Service.Mode mode, WSServiceDelegate service, Pipe pipe, BindingImpl binding) {
        super(port, mode, service, pipe, binding);
        this.jaxbcontext = jc;
        //temp temp temp - todo:check with KK on how to use pool
        //perhaps pool should be in DispatchImpl?
        //??to pool JAXB objects??
        marshallers = new Pool.Marshaller(jaxbcontext);
        unmarshallers = new Pool.Unmarshaller(jaxbcontext);
    }


    protected Object toReturnValue(Message response) {
        try {
            Unmarshaller unmarshaller = jaxbcontext.createUnmarshaller();
            switch (mode) {
                case PAYLOAD:
                    return response.<Object>readPayloadAsJAXB(unmarshaller);
                case MESSAGE:
                    Source result = response.readEnvelopeAsSource();
                    return unmarshaller.unmarshal(result);
                default:
                    throw new WebServiceException("Unrecognized dispatch mode");
            }
        } catch (JAXBException e) {
            throw new WebServiceException(e);
        }
    }

    private void setHttpRequestHeaders(MessageProperties props) {
        Map<String, List<String>> ch = new HashMap<String, List<String>>();

        List<String> ct = new ArrayList<String>();
        ct.add("text/xml");
        ch.put("Content-Type", ct);

        List<String> cte = new ArrayList<String>();
        cte.add("binary");
        ch.put("Content-Transfer-Encoding", cte);

        props.httpRequestHeaders = ch;
    }

    protected Message createMessage(Object msg) {
        assert jaxbcontext != null;
        //todo: use Pool - temp to get going
        try {
            Marshaller marshaller = jaxbcontext.createMarshaller();
            marshaller.setProperty("jaxb.fragment", Boolean.TRUE);
            return new JAXBMessage(marshaller, msg, soapVersion);
        } catch (JAXBException e) {
            throw new WebServiceException(e);
        }
    }

    @Override
    protected void setProperties(MessageProperties props, boolean isOneWay) {
        super.setProperties(props, isOneWay);

        setHttpRequestHeaders(props);
        props.otherProperties.put(JAXB_CONTEXT_PROPERTY, jaxbcontext); // KK - do we really need this?
    }
}