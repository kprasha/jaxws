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
package com.sun.xml.ws.server;

import com.sun.xml.ws.api.message.Packet;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.WebServiceContext;

/**
 * Implements {@link WebServiceContext}'s {@link MessageContext} on top of {@link Packet}.
 *
 * <p>
 * This class creates a {@link Map} view for APPLICATION scoped properties that
 * gets exposed to endpoint implementations during the invocation
 * of web methods. The implementations access this map using
 * WebServiceContext.getMessageContext().
 *
 * <p>
 * Some of the {@link Map} methods requre this class to
 * build the complete {@link Set} of properties, but we
 * try to avoid that as much as possible.
 *
 *
 * @author Jitendra Kotamraju
 */
@SuppressWarnings({"SuspiciousMethodCalls"})
final class EndpointMessageContextImpl extends AbstractMap<String,Object> implements MessageContext {

    /**
     * Lazily computed.
     */
    private Set<Map.Entry<String,Object>> entrySet;
    private final Packet packet;

    /**
     * @param packet
     *      The {@link Packet} to wrap.
     */
    public EndpointMessageContextImpl(Packet packet) {
        this.packet = packet;
    }

    @Override
    public Object get(Object key) {
        if (packet.supports(key)) {
            return packet.get(key);    // strongly typed
        }

        if (packet.getHandlerScopePropertyNames(true).contains(key)) {
            return null;            // no such application-scope property
        }

        Object v = packet.invocationProperties.get(key);
        if (v != null) {
            return v;
        }

        return packet.otherProperties.get(key);
    }

    @Override
    public Object put(String key, Object value) {
        if (packet.supports(key)) {
            return packet.put(key, value);     // strongly typed
        }
        Object old = packet.invocationProperties.get(key);
        if (old != null) {
            if (packet.getHandlerScopePropertyNames(true).contains(key)) {
                throw new IllegalArgumentException("Cannot overwrite property in HANDLER scope");
            }
            // Overwrite existing APPLICATION scoped property
            packet.invocationProperties.put(key, value);
            return old;
        }
        old = packet.otherProperties.get(key);
        if (old != null) {
            if (packet.getHandlerScopePropertyNames(true).contains(key)) {
                throw new IllegalArgumentException("Cannot overwrite property in HANDLER scope");
            }
            // Overwrite existing APPLICATION scoped property
            packet.otherProperties.put(key, value);
            return old;
        }
        // No existing property. So Add a new property
        packet.invocationProperties.put(key, value);
        return null;
    }

    @Override
    public Object remove(Object key) {
         if (packet.supports(key)) {
             return packet.remove(key);
        }
        Object old = packet.invocationProperties.get(key);
        if (old != null) {
            if (packet.getHandlerScopePropertyNames(true).contains(key)) {
                throw new IllegalArgumentException("Cannot remove property in HANDLER scope");
            }
            // Remove existing APPLICATION scoped property
            packet.invocationProperties.remove(key);
            return old;
        }
        old = packet.otherProperties.get(key);
        if (old != null) {
            if (packet.getHandlerScopePropertyNames(true).contains(key)) {
                throw new IllegalArgumentException("Cannot remove property in HANDLER scope");
            }
            // Remove existing APPLICATION scoped property
            packet.otherProperties.remove(key);
            return old;
        }
        // No existing property.
        return null;
    }

    public Set<Map.Entry<String, Object>> entrySet() {
        if (entrySet == null) {
            entrySet = new EntrySet();
        }
        return entrySet;
    }

    public void setScope(String name, MessageContext.Scope scope) {
    }

    public MessageContext.Scope getScope(String name) {
        return null;
    }

    private class EntrySet extends AbstractSet<Map.Entry<String, Object>> {

        public Iterator<Map.Entry<String, Object>> iterator() {
            return null;
        }
        public boolean contains(Object o) {
            return false;
        }
        public boolean remove(Object o) {
            return EndpointMessageContextImpl.this.remove(o) != null;
        }

        public int size() {
            return 0;
        }
        public void clear() {
            EndpointMessageContextImpl.this.clear();
        }
    }

}
