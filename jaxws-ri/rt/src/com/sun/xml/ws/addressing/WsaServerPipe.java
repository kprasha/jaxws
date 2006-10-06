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

package com.sun.xml.ws.addressing;

import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.soap.AddressingFeature;
import javax.xml.ws.handler.MessageContext;

import com.sun.xml.ws.api.EndpointAddress;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.addressing.WSEndpointReference;
import com.sun.xml.ws.api.addressing.AddressingVersion;
import com.sun.xml.ws.api.addressing.MemberSubmissionAddressingFeature;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.message.HeaderList;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.pipe.*;
import com.sun.xml.ws.api.pipe.helper.AbstractFilterPipeImpl;
import com.sun.xml.ws.api.pipe.helper.AbstractFilterTubeImpl;
import com.sun.xml.ws.transport.http.client.HttpTransportPipe;
import com.sun.xml.ws.addressing.v200408.MemberSubmissionAddressingConstants;
import com.sun.istack.NotNull;

/**
 * @author Arun Gupta
 */
public class WsaServerPipe extends AbstractFilterTubeImpl {
    final WSDLPort wsdlPort;
    final WSBinding binding;
    final WsaPipeHelper helper;

    public WsaServerPipe(WSDLPort wsdlPort, WSBinding binding, Tube next) {
        super(next);
        this.wsdlPort = wsdlPort;
        this.binding = binding;
        helper = getPipeHelper();
    }

    public WsaServerPipe(WsaServerPipe that, TubeCloner cloner) {
        super(that, cloner);
        this.wsdlPort = that.wsdlPort;
        this.binding = that.binding;
        this.helper = that.helper;
    }

    public WsaServerPipe copy(TubeCloner cloner) {
        return new WsaServerPipe(this, cloner);
    }

    public @NotNull NextAction processRequest(Packet request) {
        if(wsdlPort == null) {
            // Addressing is not enabled
            return doInvoke(next,request);
        }

        Packet p = null;
        try {
            p = helper.validateServerInboundHeaders(request);
        } catch (XMLStreamException e) {
            throw new WebServiceException(e);
        }

        // if one-way message and WS-A header processing fault has occurred,
        // then do no further processing
        if (p.getMessage() == null)
            return doReturnWith(p);

        // if there is a header processing fault
        if (p.getMessage().isFault()) {
            return doReturnWith(processFault(p, false));
        }

        // can this condition occur ?
        HeaderList hl = request.getMessage().getHeaders();
        if (hl == null)
            return doReturnWith(p);

        // TODO: Evaluate the impact of other pipes that might have
        // TODO: executed before WS-A pipe. So far RM always sends the
        // TODO: protocol response along with application response ONLY.
        AddressingVersion av = binding.getAddressingVersion();
        WSEndpointReference replyTo = hl.getReplyTo(av, binding.getSOAPVersion());
        String replyToAddress = null;
        if (replyTo != null)
            replyToAddress = replyTo.getAddress();
        WSEndpointReference faultTo = hl.getFaultTo(av, binding.getSOAPVersion());
        String faultToAddress = null;
        if (faultTo != null)
            faultToAddress = faultTo.getAddress();
        if (replyTo != null) {
            // none ReplyTo
            if (isOneWay(p) ||
                    replyToAddress.equals(av.getNoneUri()) &&
                    ((faultTo == null) ||
                            (!faultToAddress.equals(av.getAnonymousUri())))) {
                if (p.transportBackChannel != null) {
                    p.transportBackChannel.close();
                }
                return doInvoke(next,p);
            }

            // non-anonymous ReplyTo
            if (!replyToAddress.equals(av.getAnonymousUri()) &&
                    ((faultTo == null) ||
                            (!faultToAddress.equals(av.getAnonymousUri())))) {
                return doReturnWith(processNonAnonymousReply(p, replyToAddress, true));
            }
        }

        return doInvoke(next,p);
    }

    public @NotNull NextAction processResponse(Packet response) {
        if (response.getMessage() != null && response.getMessage().isFault()) {
            response = processFault(response, false);
        }
        return doReturnWith(response);
    }
    /*
    public Packet process(Packet request) {
        if(wsdlPort == null) {
            // Addressing is not enabled
            return next.process(request);
        }

        Packet p = null;
        try {
            p = helper.validateServerInboundHeaders(request);
        } catch (XMLStreamException e) {
            throw new WebServiceException(e);
        }

        if (p.getMessage() != null && p.getMessage().isFault()) {
            return processFault(p, false);
        }

        HeaderList hl = request.getMessage().getHeaders();
        if (hl == null)
            return p;

        // TODO: Evaluate the impact of other pipes that might have
        // TODO: executed before WS-A pipe. So far RM always sends the
        // TODO: protocol response along with application response ONLY.
        AddressingVersion av = binding.getAddressingVersion();
        WSEndpointReference replyTo = hl.getReplyTo(av, binding.getSOAPVersion());
        String replyToAddress = null;
        if (replyTo != null)
            replyToAddress = replyTo.getAddress();
        WSEndpointReference faultTo = hl.getFaultTo(av, binding.getSOAPVersion());
        String faultToAddress = null;
        if (faultTo != null)
            faultToAddress = faultTo.getAddress();
        if (replyTo != null) {
            // none ReplyTo
            if (isOneWay(p) ||
                    replyToAddress.equals(av.getNoneUri()) &&
                    ((faultTo == null) ||
                            (!faultToAddress.equals(av.getAnonymousUri())))) {
                if (p.transportBackChannel != null) {
                    p.transportBackChannel.close();
                }
                return next.process(p);
            }

            // non-anonymous ReplyTo
            if (!replyToAddress.equals(av.getAnonymousUri()) &&
                    ((faultTo == null) ||
                            (!faultToAddress.equals(av.getAnonymousUri())))) {
                return processNonAnonymousReply(p, replyToAddress, true);
            }
        }

        p = next.process(p);

        if (p.getMessage() != null && p.getMessage().isFault()) {
            return processFault(p, false);
        }

        return p;
    }
    */
    private boolean isOneWay(Packet p) {
        return (wsdlPort != null && p.getMessage() != null && p.getMessage().isOneWay(wsdlPort));
    }

    /**
     * Process none and non-anonymous Fault endpoints
     *
     * @param p packet
     * @param invokeEndpoint true if endpoint has been invoked, false otherwise
     * @return response packet received from endpoint
     */
    private Packet processFault(Packet p, final boolean invokeEndpoint) {
        if (p.getMessage() == null)
            return p;

        HeaderList hl = p.getMessage().getHeaders();
        if (hl == null)
            return p;

        AddressingVersion av = binding.getAddressingVersion();
        // todo: this is not request ReplyTo and FaultTo
        String replyToAddress = hl.getReplyTo(av, binding.getSOAPVersion()) == null ? null : hl.getReplyTo(av, binding.getSOAPVersion()).getAddress();
        String faultToAddress = hl.getFaultTo(av, binding.getSOAPVersion()) == null ? null : hl.getFaultTo(av, binding.getSOAPVersion()).getAddress();

        if (hl.getFaultTo(av, binding.getSOAPVersion()) == null) {
            // default FaultTo is ReplyTo

            if (hl.getReplyTo(av, binding.getSOAPVersion()) != null) {
                // if none, then fault message is not sent back
                if (replyToAddress.equals(av.getNoneUri())) {
                    if (invokeEndpoint) {
                        return p.createServerResponse(p.getMessage(), wsdlPort, binding);
                    }
                    p.transportBackChannel.close();
                } else if (!replyToAddress.equals(av.getAnonymousUri())) {
                    // non-anonymous default FaultTo
                    return processNonAnonymousReply(p, replyToAddress, invokeEndpoint);
                }
            }
        } else {
            // explicit FaultTo

            // if none, then fault message is not sent back
            if (faultToAddress.equals(av.getNoneUri())) {
                if (invokeEndpoint) {
                    return p.createServerResponse(null, wsdlPort, binding);
                }
                p.transportBackChannel.close();
            } else if (!faultToAddress.equals(av.getAnonymousUri())) {
                // non-anonymous FaultTo
                return processNonAnonymousReply(p, faultToAddress, invokeEndpoint);
            }
        }

        return p;
    }

    /**
     * Send response to non-anonymous address
     *
     * @param packet packet
     * @param uri endpoint address
     * @param invokeEndpoint true if endpoint has been invoked, false otherwise
     * @return response received from the non-anonymous endpoint
     */
    private Packet processNonAnonymousReply(final Packet packet, final String uri, final boolean invokeEndpoint) {
        if (packet.transportBackChannel != null) {
            System.out.println("Sending 202 and processing non-anonymous response");
            packet.transportBackChannel.close();
        }
        Packet response = packet;
        if (invokeEndpoint) {
            //TODO:
            //response = next.process(packet);
        }

        AddressingVersion av = binding.getAddressingVersion();
        HeaderList hl = packet.getMessage().getHeaders();
        WSEndpointReference replyTo = hl.getReplyTo(av, binding.getSOAPVersion());
        String replyToAddress = null;
        if (replyTo != null)
            replyToAddress = replyTo.getAddress();
        WSEndpointReference faultTo = hl.getFaultTo(av, binding.getSOAPVersion());
        String faultToAddress = null;
        if (faultTo != null)
            faultToAddress = faultTo.getAddress();

        if ((response != null && response.getMessage() != null && response.getMessage().isFault() &&
                faultTo != null && faultToAddress.equals(av.getNoneUri())) ||
                (replyTo != null && replyToAddress.equals(av.getNoneUri()))) {
            return packet;
        }

        // TODO: Use TransportFactory to create the appropriate Pipe ?
        if (!uri.startsWith("http")) {
            System.out.printf("Unknown protocol: \"%s\"\n", uri.substring(0, uri.indexOf("://")));
            return packet;
        }

        System.out.printf("Sending non-anonymous reply to %s\n", uri);
        HttpTransportPipe tPipe = new HttpTransportPipe(binding);
        response.endpointAddress = new EndpointAddress(URI.create(uri));
        response = tPipe.process(response);

        if (response != null) {
            Map<String, List<String>> reqHeaders = (Map<String, List<String>>) response.invocationProperties.get(MessageContext.HTTP_REQUEST_HEADERS);
            if (reqHeaders != null) {
                for (String key : reqHeaders.keySet()) {
                    System.out.print("[" + key + "]: ");
                    for (String value : reqHeaders.get(key)) {
                        System.out.print(value + " ");
                    }
                    System.out.println();
                }
            } else {
                System.out.printf("%s: null response headers\n", uri.toString());
            }
        } else {
            System.out.printf("%s: null response\n", uri);
        }

        return response;
    }

    private WsaPipeHelper getPipeHelper() {
        if(binding.isFeatureEnabled(AddressingFeature.ID)) {
            return new WsaPipeHelperImpl(wsdlPort, binding);
        } else if(binding.isFeatureEnabled(MemberSubmissionAddressingFeature.ID)) {
            return new com.sun.xml.ws.addressing.v200408.WsaPipeHelperImpl(wsdlPort, binding);
        } else {
            // Addressing is not enabled, WsaServerPipe should not be included in the pipeline
            throw new WebServiceException("Addressing is not enabled, " +
                    "WsaServerPipe should not be included in the pipeline");
        }
    }

}
