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
package com.sun.xml.ws.api.pipe;

import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.WSBinding;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;

/**
 * The reverse operation of {@link Codec}.
 *
 * <p>
 * {@link Decoder} is a non-reentrant object, meaning no two threads
 * can concurrently invoke the decode method. This allows the implementation
 * to easily reuse parser objects (as instance variables), which are costly otherwise.
 *
 *
 * <p>
 * {@link WSBinding} determines the {@link Decoder}. See {@link WSBinding#createDecoder()}.
 *
 *
 * TODO: do we need a look up table from content type to {@link Decoder}?
 *
 * TODO: do we need to be able to get a corresponding {@link Codec} from {@link Decoder}
 *       and vice versa?
 *
 * @author Kohsuke Kawaguchi
 */
public interface Decoder {
}
