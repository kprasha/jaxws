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
package com.sun.xml.ws.api.message;

import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.client.BindingProviderProperties;
import com.sun.xml.ws.util.PropertySet;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.LogicalMessageContext;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a container of a {@link Message}.
 *
 * <h2>What is a {@link Packet}?</h2>
 * <p>
 * A packet can be thought of as a frame/envelope/package that wraps
 * a {@link Message}. A packet keeps track of optional metadata (properties)
 * about a {@link Message} that doesn't go across the wire.
 * This roughly corresponds to {@link MessageContext} in the JAX-WS API.
 *
 * <p>
 * Usually a packet contains a {@link Message} in it, but sometimes
 * (such as for a reply of an one-way operation), a packet may
 * float around without a {@link Message} in it.
 *
 *
 * <h2>Properties</h2>
 * <p>
 * Information frequently used inside the JAX-WS RI
 * is stored in the strongly-typed fields. Other information is stored
 * in terms of a generic {@link Map} (see {@link #otherProperties} and
 * {@link #invocationProperties}.)
 *
 * <p>
 * Some properties need to be retained between request and response,
 * some don't. For strongly typed fields, this characteristic is
 * statically known for each of them, and propagation happens accordingly.
 * For generic information stored in {@link Map}, {@link #otherProperties}
 * stores per-message scope information (which don't carry over to
 * response {@link Packet}), and {@link #invocationProperties}
 * stores per-invocation scope information (which carries over to
 * the response.)
 *
 * <p>
 * This object is used as the backing store of {@link MessageContext}, and
 * {@link LogicalMessageContext} and {@link SOAPMessageContext} will
 * be delegating to this object for storing/retrieving values.
 *
 *
 * <h3>Relationship to request/response context</h3>
 * <p>
 * Request context is used to seed the initial values of {@link Packet}.
 * Some of those values go to strongly-typed fields, and others go to
 * {@link #invocationProperties}, as they need to be retained in the reply message.
 *
 * <p>
 * Similarly, response context is constructed from {@link Packet}.
 * (Or rather it's just a view of {@link Packet}.)
 *
 *
 * <h3>TODO</h3>
 * <ol>
 *  <li>this class needs to be cloneable since Message is copiable.
 *  <li>The three live views aren't implemented correctly. It will be
 *      more work to do so, although I'm sure it's possible.
 *  <li>{@link Property} annotation is to make it easy
 *      for {@link MessageContext} to export properties on this object,
 *      but it probably needs some clean up.
 * </ol>
 *
 * @author Kohsuke Kawaguchi
 */
public final class Packet extends PropertySet {

    /**
     * Creates a {@link Packet} that wraps a given {@link Message}.
     */
    public Packet(Message message) {
        this.message = message;
    }

    /**
     * Creates an empty {@link Packet} that doesn't have any {@link Message}.
     */
    public Packet() {
    }

    private Message message;

    /**
     * Gets the last {@link Message} set through {@link #setMessage(Message)}.
     *
     * @return
     *      may null. See the class javadoc for when it's null.
     */
    public Message getMessage() {
        return message;
    }

    /**
     * Sets a {@link Message} to this packet.
     *
     * @param message
     *      Can be null.
     */
    public void setMessage(Message message) {
        this.message = message;
    }

    /**
     * Value of {@link #HTTP_REQUEST_HEADERS} property.
     *
     * @deprecated
     *      shouldn't be computed eagerly
     */
    @Property(MessageContext.HTTP_REQUEST_HEADERS)
    public Map<String, List<String>> httpRequestHeaders;

    /**
     * Value of {@link #HTTP_RESPONSE_HEADERS} property.
     *
     * @deprecated
     *      shouldn't be computed eagerly
     */
    @Property(MessageContext.HTTP_RESPONSE_HEADERS)
    public Map<String, List<String>> httpResponseHeaders;

    /**
     * True if this message came from a transport (IOW inbound),
     * and in paricular from a "secure" transport. A transport
     * needs to set this flag appropriately.
     *
     * <p>
     * This is a requirement from the security team.
     */
    // TODO: expose this as a property
    public boolean wasTransportSecure;

    /**
     * If a message originates from a proxy stub that implements
     * a port interface, this field is set to point to that object.
     *
     * TODO: who's using this property? 
     */
    @Property(BindingProviderProperties.JAXWS_CLIENT_HANDLE_PROPERTY)
    public BindingProvider proxy;

    /**
     * The endpoint address to which this message is sent to.
     *
     * <p>
     * The JAX-WS spec allows this to be changed for each message,
     * so it's designed to be a property.
     */
    @Property(BindingProvider.ENDPOINT_ADDRESS_PROPERTY)
    public String endpointAddress;

    /**
     * The value of the SOAPAction header associated with the message.
     *
     * <p>
     * For outgoing messages, the transport may sends out this value.
     * If this field is null, the transport may choose to send <tt>""</tt>
     * (quoted empty string.)
     *
     * For incoming messages, the transport will set this field.
     * If the incoming message did not contain the SOAPAction header,
     * the transport sets this field to null.
     *
     * <p>
     * If the value is non-null, it must be always in the quoted form.
     * The value can be null.
     *
     * <p>
     * Note that the way the transport sends this value out depends on
     * transport and SOAP version.
     *
     * For HTTP transport and SOAP 1.1, BP requires that SOAPAction
     * header is present (See {@BP R2744} and {@BP R2745}.) For SOAP 1.2,
     * this is moved to the parameter of the "application/soap+xml".
     */
    @Property(BindingProviderProperties.SOAP_ACTION_PROPERTY)
    public String soapAction;

    /**
     * Indicates whether the current message is a request of
     * an one-way operation.
     *
     * <p>
     * This property is used on the client-side for
     * outbound messages, so that the producer of a {@link Message}
     * can communicate to the intermediate (and terminal) {@link Pipe}s
     * about its knowledge.
     *
     * <p>
     * When this property is {@link Boolean#TRUE}, it means that the producer of
     * the {@link Message} definitely knows that it's a request
     * {@link Message} is for an one-way operation.
     *
     * <p>
     * When this property is {@link Boolean#FALSE}, it means that the producer of
     * the {@link Message} definitely knows that it's expecting
     * a response for this message.
     *
     * <p>
     * When this property is null, it means that the producer
     * of the {@link Message} does not know if a reply is expected
     * or not.
     * (To give you some idea about when this can happen,
     * sometimes we don't have any WSDL and so we can't tell.)
     *
     * <p>
     * No other {@link Boolean} instances are allowed.
     */
    @Property(BindingProviderProperties.ONE_WAY_OPERATION)
    public Boolean isOneWay;

    /**
     * Bag to capture "other" properties that do not have
     * strongly-typed presence on this object.
     *
     * Properties in this map will have the same life span
     * as the Message itself.
     *
     * TODO: allocate this instance lazily.
     */
    public final Map<String,Object> otherProperties = new HashMap<String,Object>();

    /**
     * Bag to capture properties that are available for the whole
     * message invocation (namely on both requests and responses.)
     *
     * <p>
     * These properties are copied from a request to a response.
     * This is where we keep properties that are set by handlers.
     */
    public final Map<String,Object> invocationProperties = new HashMap<String,Object>();

    /**
     * Gets a {@link Set} that stores application-scope properties.
     *
     * These properties will be exposed to the response context.
     *
     * @param readOnly
     *      Return true if the caller only intends to read the value of this set.
     *      Internally, the {@link Set} is allocated lazily, and this flag helps
     *      optimizing the strategy.
     *
     * @return
     *      always non-null, possibly empty set that stores property names.
     */
    public final Set<String> getApplicationScopePropertyNames( boolean readOnly ) {
        Set<String> o = (Set<String>) invocationProperties.get(SCOPE_PROPERTY);
        if(o==null) {
            if(readOnly)
                return Collections.emptySet();
            o = new HashSet<String>();
            invocationProperties.put(SCOPE_PROPERTY,o);
        }
        return o;
    }

    private static final String SCOPE_PROPERTY = "com.sun.xml.ws.HandlerScope";


// completes TypedMap
    private static final Map<String,Accessor> model;

    static {
        model = parse(Packet.class);
    }

    protected Map<String, Accessor> getPropertyMap() {
        return model;
    }
}
