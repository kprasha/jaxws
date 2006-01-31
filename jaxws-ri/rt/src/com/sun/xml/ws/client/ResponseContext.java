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
package com.sun.xml.ws.client;

import com.sun.xml.ws.api.message.MessageProperties;
import com.sun.xml.ws.api.message.Message;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Implements "response context" on top of {@link MessageProperties}.
 *
 * <p>
 * This class creates a read-only {@link Map} view that
 * gets exposed to client applications after an invocation
 * is complete.
 *
 * <p>
 * The design goal of this class is to make it efficient
 * to create a new {@link ResponseContext}, at the expense
 * of making some {@link Map} operations slower. This is
 * justified because the response context is mostly just
 * used to query a few known values, and operations like
 * enumeration isn't likely.
 *
 * <p>
 * Some of the {@link Map} methods requre this class to
 * build the complete {@link Set} of properties, but we
 * try to avoid that as much as possible.
 *
 *
 * <pre>
 * TODO: are we exposing all strongly-typed fields, or
 * do they have appliation/handler scope notion?
 * </pre>
 *
 * @author Kohsuke Kawaguchi
 */
@SuppressWarnings({"SuspiciousMethodCalls"})    // IDE doesn't like me calling Map methods with key typed as Object
public class ResponseContext extends AbstractMap<String,Object> {
    private final MessageProperties props;

    /**
     * Lazily computed.
     */
    private Set<Map.Entry<String,Object>> entrySet;

    /**
     * @param props
     *      The {@link MessageProperties} to wrap.
     */
    public ResponseContext(MessageProperties props) {
        this.props = props;
    }

    /**
     * @param reply
     *      The {@link Message} whose properties we wrap.
     */
    public ResponseContext(Message reply) {
        this(reply.getProperties());
    }

    public boolean containsKey(Object key) {
        if(props.containsKey(key))
            return true;    // strongly typed

        if(props.invocationProperties.containsKey(key) || props.otherProperties.containsKey(key))
            return props.getApplicationScopePropertyNames(true).contains(key);

        return false;
    }

    public Object get(Object key) {
        if(props.containsKey(key))
            return props.get(key);    // strongly typed

        if(!props.getApplicationScopePropertyNames(true).contains(key))
            return null;            // no such application-scope property

        Object v = props.invocationProperties.get(key);
        if(v!=null)     return v;

        return props.otherProperties.get(key);
    }

    public Object put(String key, Object value) {
        // response context is read-only
        throw new UnsupportedOperationException();
    }

    public Object remove(Object key) {
        // response context is read-only
        throw new UnsupportedOperationException();
    }

    public void putAll(Map<? extends String, ? extends Object> t) {
        // response context is read-only
        throw new UnsupportedOperationException();
    }

    public void clear() {
        // response context is read-only
        throw new UnsupportedOperationException();
    }

    public Set<Entry<String, Object>> entrySet() {
        if(entrySet==null) {
            // this is where the worst case happens. we have to clone the whole properties
            // to get this view.

            // use TreeSet so that toString() sort them nicely. It's easier for apps.
            Map<String,Object> r = new HashMap<String,Object>();

            // export application-scope properties
            for (String key : props.getApplicationScopePropertyNames(true)) {
                if(containsKey(key))
                    r.put(key,get(key));
            }

            // and all strongly typed ones
            r.putAll(props.createMapView());

            entrySet = Collections.unmodifiableSet(r.entrySet());
        }

        return entrySet;
    }

}
