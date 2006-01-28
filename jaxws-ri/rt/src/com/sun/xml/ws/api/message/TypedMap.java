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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author WS Development Team
 */
public abstract class TypedMap {
    
    /** Creates a new instance of TypedMap */
    protected TypedMap() {
    }
    
    /**
     * Map representing the Fields and Methods annotated with @ContextProperty.
     * Model of {@link TypedMap} class.
     */
    public static Map<String,Property> props;
    
    /**
     * This method parses a class for fields and methods with @ContextProperty
     * and populates the Map.
     */
    public static void parse(Class clazz) {
        props = new HashMap<String,Property>();
        for (Field f : clazz.getClass().getFields()) {
            ContextProperty cp = f.getAnnotation(ContextProperty.class);
            if(cp!=null)
                props.put(cp.value(), new FieldProperty(f, cp));
        }
        
        for (Method m : clazz.getClass().getMethods()) {
            ContextProperty cp = m.getAnnotation(ContextProperty.class);
            // if(cp!=null) props.put(cp.value(), new MethodProperty(m, cp));
        }
        
    }
    
    /**
     * The interface <code>Property</code> represents an
     * entry in <code>TypedMap<code>.
     *
     * @author WS Development Team
     */
    
    protected interface Property {
        String getName();
        
        boolean hasValue(TypedMap props);
        
        Object get(TypedMap props);
        
        void set(TypedMap props, Object value);
        
    }
    
    static final class FieldProperty implements Property {
        /**
         * Field with the annotation.
         */
        private final Field f;
        
        /**
         * {@link ContextProperty} annotation on {@link #f}.
         */
        final ContextProperty annotation;
        
        protected FieldProperty(Field f, ContextProperty annotation) {
            this.f = f;
            this.annotation = annotation;
        }
        
        public String getName() {
            return annotation.value();
        }
        
        public boolean hasValue(TypedMap props) {
            return get(props)!=null;
        }
        
        public Object get(TypedMap props) {
            try {
                return f.get(props);
            } catch (IllegalAccessException e) {
                throw new AssertionError();
            }
        }
        
        public void set(TypedMap props, Object value) {
            try {
                f.set(props,value);
            } catch (IllegalAccessException e) {
                throw new AssertionError();
            }
        }
    }
    
}
