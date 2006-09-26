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

import com.sun.xml.ws.addressing.model.AddressingProperties;
import com.sun.xml.ws.api.EndpointAddress;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.model.SEIModel;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.api.pipe.PipeCloner;
import com.sun.xml.ws.transport.http.client.HttpTransportPipe;

/**
 * @author Arun Gupta
 */
public class WsaServerPipe extends WsaPipe {
    public WsaServerPipe(WSDLPort wsdlPort, WSBinding binding, Pipe next) {
        super(wsdlPort, binding, next);
    }

    public WsaServerPipe(WsaServerPipe that, PipeCloner cloner) {
        super(that, cloner);
    }

    public WsaServerPipe copy(PipeCloner cloner) {
        return new WsaServerPipe(this, cloner);
    }

    public Packet process(Packet packet) {
        // Dispatch w/o WSDL
        if (wsdlPort == null) {
            return next.process(packet);
        }

        Packet p = helper.readServerInboundHeaders(packet);

        AddressingProperties inbound = (AddressingProperties) p.invocationProperties.get(AddressingConstants.SERVER_INBOUND);

        if (p.getMessage() != null && p.getMessage().isFault()) {
            return processFault(p, inbound, false);
        }

        // TODO: Evaluate the impact of other pipes that might have
        // TODO: executed before WS-A pipe. So far RM always sends the
        // TODO: protocol response along with application response ONLY.
        if (inbound != null && inbound.getReplyTo() != null) {
            // none ReplyTo
            if (helper.getAddress(inbound.getReplyTo()).equals(helper.getNoneURI()) &&
                    ((inbound.getFaultTo() == null) ||
                            (!helper.getAddress(inbound.getFaultTo()).equals(helper.getAnonymousURI())))) {
                if (p.transportBackChannel != null) {
                    p.transportBackChannel.close();
                }
                return next.process(p);
            }

            // non-anonymous ReplyTo
            if (!helper.getAddress(inbound.getReplyTo()).equals(helper.getAnonymousURI()) &&
                    ((inbound.getFaultTo() == null) ||
                            (!helper.getAddress(inbound.getFaultTo()).equals(helper.getAnonymousURI())))) {
                return processNonAnonymousReply(p, helper.getAddress(inbound.getReplyTo()), true);
            }
        }

        p = next.process(p);
        p = helper.writeServerOutboundHeaders(p);

        if (p.getMessage() != null && p.getMessage().isFault()) {
            return processFault(p, (AddressingProperties) p.invocationProperties.get(AddressingConstants.SERVER_INBOUND), false);
        }

        return p;
    }

    /**
     * Process none and non-anonymous Fault endpoints
     *
     * @param p packet
     * @param ap addressing properties
     * @param invokeEndpoint true if endpoint has been invoked, false otherwise
     * @return response packet received from endpoint
     */
    private Packet processFault(Packet p, AddressingProperties ap, final boolean invokeEndpoint) {
        if (ap == null)
            return p;

        if (ap.getFaultTo() == null) {
            // default FaultTo is ReplyTo

            if (ap.getReplyTo() != null) {
                // if none, then fault message is not sent back
                if (helper.getAddress(ap.getReplyTo()).equals(helper.getNoneURI())) {
                    if (invokeEndpoint) {
                        return p.createResponse(null);
                    }
                    p.transportBackChannel.close();
                } else if (!helper.getAddress(ap.getReplyTo()).equals(helper.getAnonymousURI())) {
                    // non-anonymous default FaultTo
                    return processNonAnonymousReply(p, helper.getAddress(ap.getReplyTo()), invokeEndpoint);
                }
            }
        } else {
            // explicit FaultTo

            // if none, then fault message is not sent back
            if (helper.getAddress(ap.getFaultTo()).equals(helper.getNoneURI())) {
                if (invokeEndpoint) {
                    return p.createResponse(null);
                }
                p.transportBackChannel.close();
            } else if (!helper.getAddress(ap.getFaultTo()).equals(helper.getAnonymousURI())) {
                // non-anonymous FaultTo
                return processNonAnonymousReply(p, helper.getAddress(ap.getFaultTo()), invokeEndpoint);
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
        if (packet.transportBackChannel != null)
            packet.transportBackChannel.close();
        Packet response = packet;
        if (invokeEndpoint) {
            response = next.process(packet);
            response = helper.writeServerOutboundHeaders(response);
        }

//        AddressingProperties ap = (AddressingProperties)packet.invocationProperties.get(JAXWSAConstants.SERVER_ADDRESSING_PROPERTIES_OUTBOUND);
//        if (!ap.getReplyTo().getAddress().getURI().toString().equals(uri)) {
//            throw new AddressingException("ReplyTo address has changed");
//        }

        AddressingProperties ap = (AddressingProperties)packet.invocationProperties.get(AddressingConstants.SERVER_INBOUND);
        if ((response != null && response.getMessage() != null && response.getMessage().isFault() &&
                ap.getFaultTo() != null && helper.getAddress(ap.getFaultTo()).equals(helper.getNoneURI())) ||
                (ap.getReplyTo() != null && helper.getAddress(ap.getReplyTo()).equals(helper.getNoneURI()))) {
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
//            if (response.httpResponseHeaders != null) {
//                for (String headerKey : response.httpResponseHeaders.keySet()) {
//                    System.out.printf(headerKey + ":");
//                    for (String header : response.httpResponseHeaders.get(headerKey)) {
//                        System.out.printf("[" + header + "]");
//                    }
//                    System.out.println();
//                }
//            } else {
//                System.out.printf("%s: null response headers\n", uri.toString());
//            }
        } else {
            System.out.printf("%s: null response\n", uri);
        }

        return response;
    }

}
