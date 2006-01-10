package com.sun.xml.ws.client.port;

import com.sun.xml.bind.api.Bridge;
import com.sun.xml.bind.api.CompositeStructure;
import com.sun.xml.ws.model.WrapperParameter;
import com.sun.xml.ws.sandbox.api.model.Parameter;
import com.sun.xml.ws.sandbox.api.model.RuntimeModel;

import javax.xml.ws.Holder;
import java.util.List;

/**
 * Builds a JAXB object that represents the payload.
 *
 * @see MessageFiller
 * @author Kohsuke Kawaguchi
 */
abstract class BodyBuilder {
    /**
     * Builds a JAXB object that becomes the payload.
     */
    abstract Object build(Object[] methodArgs);

    /**
     * This object determines the binding of the object returned
     * from {@link #build(Object[])}.
     */
    final Bridge bridge;

    protected BodyBuilder(Bridge bridge) {
        assert bridge!=null;
        this.bridge = bridge;
    }

    /**
     * Used to create a payload JAXB object just by taking
     * one of the parameters.
     */
    final static class Bare extends BodyBuilder {
        /**
         * The index of the method invocation parameters that goes into the payload.
         */
        private final int methodPos;

        private final ValueGetter getter;

        /**
         * Creates a {@link BodyBuilder} from a bare parameter.
         */
        Bare(Parameter p, RuntimeModel model) {
            super(model.getBridge(p.getTypeReference()));
            this.methodPos = p.getIndex();
            this.getter = ValueGetter.get(p);
        }

        /**
         * Picks up an object from the method arguments and uses it.
         */
        Object build(Object[] methodArgs) {
            return getter.get(methodArgs[methodPos]);
        }
    }

    /**
     * Used to create a payload JAXB object by wrapping
     * multiple parameters into one bean.
     */
    final static class Wrapped extends BodyBuilder {
        /**
         * How does each wrapped parameter binds to XML?
         */
        private final Bridge[] parameterBridges;

        /**
         * Where in the method argument list do they come from?
         */
        private final int[] indices;

        /**
         * Abstracts away the {@link Holder} handling when touching method arguments.
         */
        private final ValueGetter[] getters;

        /**
         * Creates a {@link BodyBuilder} from a {@link WrapperParameter}.
         */
        Wrapped(WrapperParameter wp, RuntimeModel model) {
            super(model.getBridge(wp.getTypeReference()));
            // we'll use CompositeStructure to pack requests
            assert wp.getTypeReference().type==CompositeStructure.class;

            List<Parameter> children = wp.getWrapperChildren();

            parameterBridges = new Bridge[children.size()];
            for( int i=0; i<parameterBridges.length; i++ )
                parameterBridges[i] = model.getBridge(children.get(i).getTypeReference());

            indices = new int[children.size()];
            getters = new ValueGetter[children.size()];
            for( int i=0; i<indices.length; i++ ) {
                Parameter p = children.get(i);
                indices[i] = p.getIndex();
                getters[i] = ValueGetter.get(p);
            }
        }

        /**
         * Packs a bunch of arguments intoa {@link CompositeStructure}.
         */
        CompositeStructure build(Object[] methodArgs) {
            CompositeStructure cs = new CompositeStructure();
            cs.bridges = parameterBridges;
            cs.values = new Object[parameterBridges.length];

            // fill in wrapped parameters from methodArgs
            for( int i=indices.length-1; i>=0; i-- )
                cs.values[i] = getters[i].get(methodArgs[indices[i]]);

            return cs;
        }
    }
}
