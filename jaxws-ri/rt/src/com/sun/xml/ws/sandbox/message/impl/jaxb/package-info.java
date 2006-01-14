/**
 * {@link Message} implementation for JAXB.
 *
 * <pre>
 * TODO:
 *      Because a producer of a message doesn't generally know
 *      when a message is consumed, it's difficult for
 *      the caller to do a proper instance caching. Perhaps
 *      there should be a layer around JAXBContext that does that?
 * </pre>
 */
package com.sun.xml.ws.sandbox.message.impl.jaxb;

import com.sun.xml.ws.api.message.Message;