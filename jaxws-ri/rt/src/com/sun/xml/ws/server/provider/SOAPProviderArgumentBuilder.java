package com.sun.xml.ws.server.provider;

import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Messages;
import com.sun.xml.ws.fault.SOAPFaultBuilder;

import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Source;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;

abstract class SOAPProviderArgumentBuilder<T> extends ProviderArgumentsBuilder<T> {
    protected final SOAPVersion soapVersion;

    private SOAPProviderArgumentBuilder(SOAPVersion soapVersion) {
        this.soapVersion = soapVersion;
    }

    static SOAPProviderArgumentBuilder create(ProviderEndpointModel model, SOAPVersion soapVersion) {
        if (model.getServiceMode() == Service.Mode.PAYLOAD) {
            return new PayloadSource(soapVersion);
        } else {
            return model.isSource() ? new MessageSource(soapVersion) : new SOAPMessageParameter(soapVersion);
        }
    }

    private static final class PayloadSource extends SOAPProviderArgumentBuilder<Source> {
        PayloadSource(SOAPVersion soapVersion) {
            super(soapVersion);
        }

        public Source getParameter(Message msg) {
            return msg.readPayloadAsSource();
        }

        public Message getResponse(Source source) {
            return Messages.createUsingPayload(source, soapVersion);
        }

        public Message getResponseMessage(Exception e) {
            return SOAPFaultBuilder.createSOAPFaultMessage(soapVersion, null, e);
        }
    }

    private static final class MessageSource extends SOAPProviderArgumentBuilder<Source> {
        MessageSource(SOAPVersion soapVersion) {
            super(soapVersion);
        }

        public Source getParameter(Message msg) {
            return msg.readEnvelopeAsSource();
        }

        public Message getResponse(Source source) {
            return Messages.create(source, soapVersion);
        }

        protected Message getResponseMessage(Exception e) {
            return SOAPFaultBuilder.createSOAPFaultMessage(soapVersion, null, e);
        }
    }

    private static final class SOAPMessageParameter extends SOAPProviderArgumentBuilder<SOAPMessage> {
        SOAPMessageParameter(SOAPVersion soapVersion) {
            super(soapVersion);
        }

        public SOAPMessage getParameter(Message msg) {
            try {
                return msg.readAsSOAPMessage();
            } catch (SOAPException se) {
                throw new WebServiceException(se);
            }
        }

        public Message getResponse(SOAPMessage soapMsg) {
            return Messages.create(soapMsg);
        }

        protected Message getResponseMessage(Exception e) {
            return SOAPFaultBuilder.createSOAPFaultMessage(soapVersion, null, e);
        }
    }

}
