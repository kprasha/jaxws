package com.sun.xml.ws.server.provider;

import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Messages;
import com.sun.xml.ws.api.message.Packet;
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

    protected void updateResponse(Packet p, Exception e) {
        // Nothing to do in SOAP binding
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

        protected Source getParameter(Packet packet) {
            return packet.getMessage().readPayloadAsSource();
        }

        protected Message getResponseMessage(Source source,Packet packet) {
            return Messages.createUsingPayload(source, soapVersion);
        }

        protected Message getResponseMessage(Exception e, Packet packet) {
            return SOAPFaultBuilder.createSOAPFaultMessage(soapVersion, null, e);
        }

    }

    private static final class MessageSource extends SOAPProviderArgumentBuilder<Source> {
        MessageSource(SOAPVersion soapVersion) {
            super(soapVersion);
        }

        protected Source getParameter(Packet packet) {
            return packet.getMessage().readEnvelopeAsSource();
        }

        protected Message getResponseMessage(Source source, Packet packet) {
            return Messages.create(source, soapVersion);
        }

        protected Message getResponseMessage(Exception e, Packet packet) {
            return SOAPFaultBuilder.createSOAPFaultMessage(soapVersion, null, e);
        }
    }

    private static final class SOAPMessageParameter extends SOAPProviderArgumentBuilder<SOAPMessage> {
        SOAPMessageParameter(SOAPVersion soapVersion) {
            super(soapVersion);
        }

        protected SOAPMessage getParameter(Packet packet) {
            try {
                return packet.getMessage().readAsSOAPMessage(packet, true);
            } catch (SOAPException se) {
                throw new WebServiceException(se);
            }
        }

        protected Message getResponseMessage(SOAPMessage soapMsg, Packet packet) {
            return Messages.create(soapMsg);
        }

        protected Message getResponseMessage(Exception e, Packet packet) {
            return SOAPFaultBuilder.createSOAPFaultMessage(soapVersion, null, e);
        }
    }

}
