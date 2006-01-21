package com.sun.xml.ws.client.port;

import com.sun.xml.bind.api.Bridge;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.SOAPVersion;

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
        private final SyncMethodHandler owner;
        private final ValueGetter getter;

        protected Header(SyncMethodHandler owner, int methodPos, SOAPVersion ver, Bridge bridge, ValueGetter getter) {
            super(methodPos);
            this.owner = owner;
            this.ver = ver;
            this.bridge = bridge;
            this.getter = getter;
        }

        void fillIn(Object[] methodArgs, Message msg) {
            Object value = getter.get(methodArgs[methodPos]);
            msg.getHeaders().add(ver.createJAXBHeader(
                bridge, owner.owner.bridgeContexts.take(), value));
        }
    }
}
