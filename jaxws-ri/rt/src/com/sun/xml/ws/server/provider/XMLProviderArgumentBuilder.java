package com.sun.xml.ws.server.provider;

import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Messages;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.encoding.xml.XMLMessage;

import javax.activation.DataSource;
import javax.xml.transform.Source;
import javax.xml.ws.Service;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.http.HTTPException;


abstract class XMLProviderArgumentBuilder<T> extends ProviderArgumentsBuilder<T> {

    protected void updateResponse(Packet response, Exception e) {
        if (e instanceof HTTPException) {
            if (response.supports(MessageContext.HTTP_RESPONSE_CODE)) {
                response.put(MessageContext.HTTP_RESPONSE_CODE, ((HTTPException)e).getStatusCode());
            }
        }
    }

    static XMLProviderArgumentBuilder create(ProviderEndpointModel model) {
        if (model.getServiceMode() == Service.Mode.PAYLOAD) {
            return new PayloadSource();
        } else {
            return model.isSource() ? new PayloadSource() : new DataSourceParameter();
        }
    }

    private static final class PayloadSource extends XMLProviderArgumentBuilder<Source> {
        public Source getParameter(Message msg) {
            return msg.readPayloadAsSource();
        }

        public Message getResponse(Source source) {
            return Messages.createUsingPayload(source, SOAPVersion.SOAP_11);
        }

        protected Message getResponseMessage(Exception e) {
            return XMLMessage.create(e);
        }
    }

    private static final class DataSourceParameter extends XMLProviderArgumentBuilder<DataSource> {
        public DataSource getParameter(Message msg) {
            return (msg instanceof XMLMessage.MessageDataSource)
                    ? ((XMLMessage.MessageDataSource) msg).getDataSource()
                    : XMLMessage.getDataSource(msg);
        }

        public Message getResponse(DataSource ds) {
            return XMLMessage.create(ds);
        }

        protected Message getResponseMessage(Exception e) {
            return XMLMessage.create(e);
        }
    }

}
