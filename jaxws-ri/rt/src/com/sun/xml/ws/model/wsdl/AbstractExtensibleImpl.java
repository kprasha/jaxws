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

package com.sun.xml.ws.model.wsdl;

import com.sun.xml.ws.api.model.wsdl.WSDLExtensible;
import com.sun.xml.ws.api.model.wsdl.WSDLExtension;

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;

/**
 * All the WSDL 1.1 elements that are extensible should subclass from this abstract implementation of
 * {@link WSDLExtensible} interface.
 *
 * @author Vivek Pandey
 * @author Kohsuke Kawaguchi
 */
abstract public class AbstractExtensibleImpl implements WSDLExtensible {
    protected final Set<WSDLExtension> extensions = new HashSet<WSDLExtension>();

    public final Iterable<WSDLExtension> getExtensions() {
        return extensions;
    }

    public final <T extends WSDLExtension> Iterable<T> getExtensions(Class<T> type) {
        // TODO: this is a rather stupid implementation
        List<T> r = new ArrayList<T>(extensions.size());
        for (WSDLExtension e : extensions) {
            if(type.isInstance(e))
                r.add(type.cast(e));
        }
        return r;
    }

    public <T extends WSDLExtension> T getExtension(Class<T> type) {
        for (WSDLExtension e : extensions) {
            if(type.isInstance(e))
                return type.cast(e);
        }
        return null;
    }

    public void addExtension(WSDLExtension ex) {
        if(ex==null)
            // I don't trust plugins. So let's always check it, instead of making this an assertion
            throw new IllegalArgumentException();
        extensions.add(ex);
    }
}
