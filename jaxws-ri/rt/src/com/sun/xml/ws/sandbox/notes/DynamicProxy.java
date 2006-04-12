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

package com.sun.xml.ws.sandbox.notes;

import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;

/**
 * Note about possible dynamic proxy improvement.
 *
 * <p>
 * Today, we use {@link Proxy} and {@link InvocationHandler} as the first
 * step in the client side. But this is inefficient, because we have to
 * check what method was invoked in {@link InvocationHandler} and
 * do the processing differently depending on it.
 *
 * <p>
 * It would be better if we can implement each interface method of the SEI
 * differently. In that way, we can hard-code the knowledge of the method
 * signature and the invocation mode (async or not) into the code, thereby
 * eliminating the needs to look up a table to determine the processing
 * next.
 *
 * <p>
 * This doesn't have to happen right away, but it's an improvement
 * we can do if it serves us.
 */
public class DynamicProxy {
}
