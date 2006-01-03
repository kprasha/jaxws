package com.sun.xml.ws.client.port;

import com.sun.xml.bind.api.CompositeStructure;

/**
 * Moves a method parameter into the appropriate position of
 * {@link CompositeStructure}.
 *
 * <p>
 * This object is used to build a {@link CompositeStructure} that
 * represents the payload of a message from <tt>Object[]</tt> that
 * represents arguments to a method.
 *
 * @author Kohsuke Kawaguchi
 * @see MessageFiller
 */
abstract class BodySetter {
    /**
     * The index of the method invocation parameters that this object looks for.
     */
    protected final int methodPos;
    /**
     * The index inside the payload.
     */
    protected final int messagePos;

    BodySetter(int methodPos, int messagePos) {
        this.methodPos = methodPos;
        this.messagePos = messagePos;
    }

    /**
     * Picks up an object from the method arguments and sets it
     * to the right place inside {@link CompositeStructure}.
     */
    abstract void set( Object[] methodArgs, CompositeStructure msg );

    /**
     * {@link BodySetter} for a plain argument.
     *
     * This class just moves an argument into a {@link CompositeStructure}.
     */
    static final class Plain extends BodySetter {
        public Plain(int methodPos, int messagePos) {
            super(methodPos, messagePos);
        }

        void set( Object[] methodArgs, CompositeStructure msg ) {
            msg.values[messagePos] = methodArgs[methodPos];
        }
    }

    /**
     * {@link BodySetter} for a {@link javax.xml.ws.Holder} argument.
     *
     * This class moves the value inside a {@link Holder} into
     * a {@link CompositeStructure}.
     */
    static final class Holder extends BodySetter {
        public Holder(int methodPos, int messagePos) {
            super(methodPos, messagePos);
        }

        void set( Object[] methodArgs, CompositeStructure msg ) {
            msg.values[messagePos] = ((javax.xml.ws.Holder)methodArgs[methodPos]).value;
        }
    }
}
