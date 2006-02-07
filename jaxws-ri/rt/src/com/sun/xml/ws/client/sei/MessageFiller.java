package com.sun.xml.ws.client.sei;

import com.sun.xml.bind.api.Bridge;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.message.Headers;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.model.SEIModel;

/**
 * Puts a non-payload message parameter to {@link Message}.
 *
 * <p>
 * Instance of this class is used to handle header parameters and attachment parameters.
 * They add things to {@link Message}.
 *
 * @see BodyBuilder
 * @author Kohsuke Kawaguchi
 */
abstract class MessageFiller {

    /**
     * The index of the method invocation parameters that this object looks for.
     */
    protected final int methodPos;

    protected MessageFiller( int methodPos) {
        this.methodPos = methodPos;
    }

    /**
     * Moves an argument of a method invocation into a {@link Message}.
     */
    abstract void fillIn(Object[] methodArgs, Message msg);

    /**
     * Adds a parameter as an header.
     */
    static final class Header extends MessageFiller {
        private final SOAPVersion ver;
        private final Bridge bridge;
        private final SEIModel model;
        private final ValueGetter getter;

        protected Header(SEIModel model, int methodPos, SOAPVersion ver, Bridge bridge, ValueGetter getter) {
            super(methodPos);
            this.model = model;
            this.ver = ver;
            this.bridge = bridge;
            this.getter = getter;
        }

        void fillIn(Object[] methodArgs, Message msg) {
            Object value = getter.get(methodArgs[methodPos]);
            msg.getHeaders().add(Headers.create(ver,
                bridge, model.getBridgeContext(), value));
        }
    }
}
