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
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.xml.ws.server.servlet;

import com.sun.istack.NotNull;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.developer.servlet.HttpSessionScope;
import com.sun.xml.ws.server.AbstractMultiInstanceResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.MessageContext;

/**
 * Instance resolver that ties a service instance per {@link HttpSession}.
 *
 * TODO: how do we dispose instances?
 *
 * @author Kohsuke Kawaguchi
 */
public class HttpSessionInstanceResolver<T> extends AbstractMultiInstanceResolver<T> {
    public HttpSessionInstanceResolver(@NotNull Class<T> clazz) {
        super(clazz);
    }

    public @NotNull T resolve(Packet request) {
        HttpServletRequest sr = (HttpServletRequest) request.get(MessageContext.SERVLET_REQUEST);
        if(sr==null)
            throw new WebServiceException(
                clazz+" has @"+ HttpSessionScope.class.getSimpleName()+" but it's deployed on non-servlet endpoint");
        
        HttpSession session = sr.getSession();
        T o = clazz.cast(session.getAttribute(clazz.getName()));
        if(o==null) {
            o = create();
            session.setAttribute(clazz.getName(),o);
        }
        return o;
    }
}
