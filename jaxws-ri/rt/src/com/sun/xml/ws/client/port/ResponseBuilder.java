package com.sun.xml.ws.client.port;

import com.sun.xml.bind.api.AccessorException;
import com.sun.xml.bind.api.Bridge;
import com.sun.xml.bind.api.BridgeContext;
import com.sun.xml.bind.api.RawAccessor;
import com.sun.xml.bind.api.CompositeStructure;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.model.ParameterImpl;
import com.sun.xml.ws.model.WrapperParameter;
import com.sun.xml.ws.streaming.XMLStreamReaderUtil;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.ws.Holder;
import javax.xml.ws.WebServiceException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Reads a response {@link Message}, disassembles it, and moves obtained Java values
 * to the expected places.
 *
 * @author Kohsuke Kawaguchi
 */
interface ResponseBuilder {
    /**
     * Reads a response {@link Message}, disassembles it, and moves obtained Java values
     * to the expected places.
     *
     * @param reply
     *      The reply {@link Message} to be de-composed.
     * @param args
     *      The Java arguments given to the SEI method invocation.
     *      Some parts of the reply message may be set to {@link Holder}s in the arguments.
     * @param context
     *      This object is used to unmarshal the reply message to Java objects.
     * @return
     *      If a part of the reply message is returned as a return value from
     *      the SEI method, this method returns that value. Otherwise null.
     * @throws JAXBException
     *      if there's an error during unmarshalling the reply message.
     * @throws XMLStreamException
     *      if there's an error during unmarshalling the reply message.
     */
    Object readResponse( Message reply, Object[] args, BridgeContext context ) throws JAXBException, XMLStreamException;

    /**
     * The singleton instance that produces no response value.
     * Used for operations that doesn't have any output.
     */
    static final ResponseBuilder NONE = new None();

    static final class None implements ResponseBuilder {
        private final ValueSetter setter;

        public None() {
            this.setter = null;
        }

        public None(ValueSetter setter){
            this.setter = setter;
        }
        public Object readResponse(Message msg, Object[] args, BridgeContext context) {
            return setter.put(null, args);
        }
    }

    /**
     * {@link ResponseBuilder} that is a composition of multiple
     * {@link ResponseBuilder}s.
     *
     * <p>
     * Sometimes we need to look at multiple parts of the reply message
     * (say, two header params, one body param, and three attachments, etc.)
     * and that's when this object is used to combine multiple {@link ResponseBuilder}s
     * (that each responsible for handling one part).
     *
     * <p>
     * The model guarantees that only at most one {@link ResponseBuilder} will
     * return a value as a return value (and everything else has to go to
     * {@link Holder}s.)
     */
    static final class Composite implements ResponseBuilder {
        private final ResponseBuilder[] builders;

        public Composite(ResponseBuilder... builders) {
            this.builders = builders;
        }

        public Composite(Collection<? extends ResponseBuilder> builders) {
            this(builders.toArray(new ResponseBuilder[builders.size()]));
        }

        public Object readResponse(Message msg, Object[] args, BridgeContext context) throws JAXBException, XMLStreamException {
            Object retVal = null;
            for (ResponseBuilder builder : builders) {
                Object r = builder.readResponse(msg,args,context);
                // there's only at most one ResponseBuilder that returns a value.
                if(r!=null) {
                    assert retVal==null;
                    retVal = r;
                }
            }
            return retVal;
        }
    }

    // TODO: attachment

    /**
     * Reads a header into a JAXB object.
     */
    static final class Header implements ResponseBuilder {
        private final Bridge<?> bridge;
        private final ValueSetter setter;
        private final QName headerName;

        /**
         * @param name
         *      The name of the header element.
         * @param bridge
         *      specifies how to unmarshal a header into a JAXB object.
         * @param setter
         *      specifies how the obtained value is returned to the client.
         */
        public Header(QName name, Bridge<?> bridge, ValueSetter setter) {
            this.headerName = name;
            this.bridge = bridge;
            this.setter = setter;
        }

        public Object readResponse(Message msg, Object[] args, BridgeContext context) throws JAXBException {
            com.sun.xml.ws.api.message.Header header =
                msg.getHeaders().get(headerName.getNamespaceURI(), headerName.getLocalPart());

            if(header!=null)
                return setter.put( header.readAsJAXB(bridge,context), args );
            else
                // header not found.
                return null;
        }
    }

    /**
     * Reads the whole payload into a single JAXB bean.
     */
    static final class Body implements ResponseBuilder {
        private final Bridge<?> bridge;
        private final ValueSetter setter;

        /**
         * @param bridge
         *      specifies how to unmarshal the payload into a JAXB object.
         * @param setter
         *      specifies how the obtained value is returned to the client.
         */
        public Body(Bridge<?> bridge, ValueSetter setter) {
            this.bridge = bridge;
            this.setter = setter;
        }

        public Object readResponse(Message msg, Object[] args, BridgeContext context) throws JAXBException {
            return setter.put( msg.readPayloadAsJAXB(bridge,context), args );
        }
    }

    /**
     * Treats a payload as multiple parts wrapped into one element,
     * and processes all such wrapped parts.
     */
    static final class DocLit implements ResponseBuilder {
        /**
         * {@link PartBuilder} keyed by the element name (inside the wrapper element.)
         */
        private final PartBuilder[] parts;

        private final Bridge wrapper;

        public DocLit(WrapperParameter wp) {
            wrapper = wp.getBridge();
            Class wrapperType = (Class) wrapper.getTypeReference().type;

            List<PartBuilder> parts = new ArrayList<PartBuilder>();

            List<ParameterImpl> children = wp.getWrapperChildren();
            for (ParameterImpl p : children) {
                if(p.isIN())
                    continue;
                QName name = p.getName();
                try {
                    parts.add( new PartBuilder(
                        wp.getOwner().getJAXBContext().getElementPropertyAccessor(
                            wrapperType,
                            name.getNamespaceURI(),
                            p.getName().getLocalPart()),
                        ValueSetter.get(p)
                    ));
                } catch (JAXBException e) {
                    throw new WebServiceException(  // TODO: i18n
                        wrapperType+" do not have a property of the name "+name,e);
                }
            }

            this.parts = parts.toArray(new PartBuilder[parts.size()]);
        }

        public Object readResponse(Message msg, Object[] args, BridgeContext context) throws JAXBException, XMLStreamException {
            Object retVal = null;

            XMLStreamReader reader = msg.readPayload();
            Object wrapperBean = wrapper.unmarshal(context, reader);

            try {
                for (PartBuilder part : parts) {
                    Object o = part.readResponse(args,wrapperBean);
                    // there's only at most one ResponseBuilder that returns a value.
                    // TODO: reorder parts so that the return value comes at the end.
                    if(o!=null) {
                        assert retVal==null;
                        retVal = o;
                    }
                }
            } catch (AccessorException e) {
                // this can happen when the set method throw a checked exception or something like that
                throw new WebServiceException(e);    // TODO:i18n
            }

            // we are done with the body
            reader.close();

            return retVal;
        }

        /**
         * Unmarshals each wrapped part into a JAXB object and moves it
         * to the expected place.
         */
        static final class PartBuilder {
            private final RawAccessor accessor;
            private final ValueSetter setter;

            /**
             * @param accessor
             *      specifies which portion of the wrapper bean to obtain the value from.
             * @param setter
             *      specifies how the obtained value is returned to the client.
             */
            public PartBuilder(RawAccessor accessor, ValueSetter setter) {
                this.accessor = accessor;
                this.setter = setter;
                assert accessor!=null && setter!=null;
            }

            final Object readResponse( Object[] args, Object wrapperBean ) throws AccessorException {
                Object obj = accessor.get(wrapperBean);
                return setter.put(obj,args);
            }


        }
    }

    /**
     * Treats a payload as multiple parts wrapped into one element,
     * and processes all such wrapped parts.
     */
    static final class RpcLit implements ResponseBuilder {
        /**
         * {@link PartBuilder} keyed by the element name (inside the wrapper element.)
         */
        private final Map<QName,PartBuilder> parts = new HashMap<QName,PartBuilder>();

        private QName wrapperName;

        public RpcLit(WrapperParameter wp) {
            assert wp.getTypeReference().type== CompositeStructure.class;

            wrapperName = wp.getName();
            List<ParameterImpl> children = wp.getWrapperChildren();
            for (ParameterImpl p : children) {
                parts.put( p.getName(), new PartBuilder(
                    p.getBridge(), ValueSetter.get(p)
                ));
            }
        }

        public Object readResponse(Message msg, Object[] args, BridgeContext context) throws JAXBException, XMLStreamException {
            Object retVal = null;

            XMLStreamReader reader = msg.readPayload();
            if (!reader.getName().equals(wrapperName))
                throw new WebServiceException( // TODO: i18n
                    "Unexpected response element "+reader.getName()+" expected: "+wrapperName);
            reader.nextTag();

            while(reader.getEventType()==XMLStreamReader.START_ELEMENT) {
                // TODO: QName has a performance issue
                PartBuilder part = parts.get(reader.getName());
                if(part==null) {
                    // no corresponding part found. ignore
                    XMLStreamReaderUtil.skipElement(reader);
                    reader.nextTag();
                } else {
                    Object o = part.readResponse(args,reader,context);
                    // there's only at most one ResponseBuilder that returns a value.
                    if(o!=null) {
                        assert retVal==null;
                        retVal = o;
                    }
                }
            }

            // we are done with the body
            reader.close();

            return retVal;
        }

        /**
         * Unmarshals each wrapped part into a JAXB object and moves it
         * to the expected place.
         */
        static final class PartBuilder {
            private final Bridge bridge;
            private final ValueSetter setter;

            /**
             * @param bridge
             *      specifies how the part is unmarshalled.
             * @param setter
             *      specifies how the obtained value is returned to the client.
             */
            public PartBuilder(Bridge bridge, ValueSetter setter) {
                this.bridge = bridge;
                this.setter = setter;
            }

            final Object readResponse( Object[] args, XMLStreamReader r, BridgeContext context ) throws JAXBException {
                Object obj = bridge.unmarshal(context,r);
                return setter.put(obj,args);
            }


        }
    }
}
