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
final class BodySetter {
    /**
     * The index of the method invocation parameters that this object looks for.
     */
    private final int methodPos;
    /**
     * The index inside the payload.
     */
    private final int messagePos;

    private final ValueGetter getter;

    BodySetter(int methodPos, int messagePos, ValueGetter getter) {
        this.methodPos = methodPos;
        this.messagePos = messagePos;
        this.getter = getter;
    }

    /**
     * Picks up an object from the method arguments and sets it
     * to the right place inside {@link CompositeStructure}.
     */
    void set( Object[] methodArgs, CompositeStructure msg ) {
        msg.values[messagePos] = getter.get(methodArgs[methodPos]);
    }
}
