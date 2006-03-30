package com.sun.xml.ws.api;

import com.sun.istack.NotNull;
import com.sun.xml.ws.api.message.Message;
import com.sun.xml.ws.api.pipe.Decoder;
import com.sun.xml.ws.api.pipe.Encoder;
import com.sun.xml.ws.api.pipe.Pipe;
import com.sun.xml.ws.sandbox.impl.DecoderFacade;
import com.sun.xml.ws.sandbox.impl.EncoderFacade;
import com.sun.xml.ws.sandbox.impl.TestDecoderImpl;
import com.sun.xml.ws.sandbox.impl.TestEncoderImpl;
import com.sun.xml.ws.util.ServiceFinder;

import javax.xml.ws.BindingType;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.http.HTTPBinding;
import javax.xml.ws.soap.SOAPBinding;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Map;
import java.util.HashMap;
import java.io.UnsupportedEncodingException;

/**
 * Parsed binding ID string.
 *
 * <p>
 * {@link BindingID} is an immutable object that represents a binding ID,
 * much like how {@link URL} is a representation of an URL.
 * Like {@link URL}, this class offers a bunch of methods that let you
 * query various traits/properties of a binding ID.
 *
 * <p>
 * {@link BindingID} is extensible; one can plug in a parser from
 * {@link String} to {@link BindingID} to interpret binding IDs that
 * the JAX-WS RI does no a-priori knowledge of.
 * Technologies such as Tango uses this to make the JAX-WS RI understand
 * binding IDs defined in their world.
 *
 * Such technologies are free to extend this class and expose more characterstics.
 *
 * <p>
 * Even though this class defines a few well known constants, {@link BindingID}
 * instances do not necessarily have singleton semantics. Use {@link #equals(Object)}
 * for the comparison.
 *
 * <h3>{@link BindingID} and {@link WSBinding}</h3>
 * <p>
 * {@link WSBinding} is mutable and represents a particular "use" of a {@link BindingID}.
 * As such, it has state like a list of {@link Handler}s, which are inherently local
 * to a particular usage. For example, if you have two proxies, you need two instances.
 *
 * {@link BindingID}, OTOH, is immutable and thus the single instance
 * that represents "SOAP1.2/HTTP" can be shared and reused by all proxies in the same VM.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class BindingID {
    /**
     * Gets the SOAP version of this binding.
     *
     * TODO: clarify what to do with XML/HTTP binding
     *
     * @return
     *      If the binding is using SOAP, this method returns
     *      a {@link SOAPVersion} constant.
     *
     *      If the binding is not based on SOAP, this method
     *      returns null. See {@link Message} for how a non-SOAP
     *      binding shall be handled by {@link Pipe}s.
     */
    public abstract SOAPVersion getSOAPVersion();

    /**
     * Creates a new {@link Encoder} for this binding.
     *
     * @param binding
     *      Ocassionally some aspects of binding can be overridden by
     *      {@link WSBinding} at runtime by users, so some {@link Encoder}s
     *      need to have access to {@link WSBinding} that it's working for.
     */
    public abstract @NotNull Encoder createEncoder(@NotNull WSBinding binding);

    /**
     * Creates a new {@link Decoder} for this binding.
     *
     * @param binding
     *      Ocassionally some aspects of binding can be overridden by
     *      {@link WSBinding} at runtime by users, so some {@link Decoder}s
     *      need to have access to {@link WSBinding} that it's working for.
     */
    public abstract @NotNull Decoder createDecoder(@NotNull WSBinding binding);

    /**
     * Gets the binding ID, which uniquely identifies the binding.
     *
     * <p>
     * The relevant specs define the binding IDs and what they mean.
     * The ID is used in many places to identify the kind of binding
     * (such as SOAP1.1, SOAP1.2, REST, ...)
     *
     * @return
     *      Always non-null same value.
     */
    public abstract String toString();

    /**
     * Returns true if this binding implies using MTOM.
     *
     * <p>
     * Note that MTOM can be enabled/disabled at runtime through
     * {@link WSBinding}, so this value merely controls how things
     * are configured by default.
     */
    public boolean isMTOMEnabled() {
        return false;
    }

    /**
     * Returns true if this binding can generate WSDL.
     *
     * <p>
     * I'm simply "transcoding" the old code and it had this notion.
     * SOAP 1.1 and "XSOAP 1.2" (whatever that is) is supposed to return true
     * from this method, but nobody else should.
     *
     * <p>
     * Someone please explain to me what this property is all about!
     */
    @Deprecated
    public boolean canGenerateWSDL() {
        return false;
    }

    /**
     * Returns a parameter of this binding ID.
     *
     * <p>
     * Some binding ID, such as those for SOAP/HTTP, uses the URL
     * query syntax (like <tt>?mtom=true</tt>) to control
     * the optional part of the binding. This method obtains
     * the value for such optional parts.
     *
     * <p>
     * For implementors of the derived classes, if your binding ID
     * does not define such optional parts (such as the XML/HTTP binding ID),
     * then you should simply return the specified default value
     * (which is what this implementation does.)
     *
     * @param parameterName
     *      The parameter name, such as "mtom" in the above example.
     * @param defaultValue
     *      If this binding ID doesn't have the specified parameter explicitly,
     *      this value will be returned.
     *
     * @return
     *      the value of the parameter, if it's present (such as "true"
     *      in the above example.) If not present, this method returns
     *      the {@code defaultValue}.
     */
    public String getParameter(String parameterName, String defaultValue) {
        return defaultValue;
    }

    /**
     * Compares the equality based on {@link #toString()}.
     */
    public boolean equals(Object obj) {
        if(!(obj instanceof BindingID))
            return false;
        return toString().equals(obj.toString());
    }

    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * Parses a binding ID string into a {@link BindingID} object.
     *
     * <p>
     * This method first checks for a few known values and then delegate
     * the parsing to {@link BindingIDFactory}.
     *
     * <p>
     * If parsing succeeds this method returns a value. Otherwise
     * throws {@link WebServiceException}.
     *
     * @throws WebServiceException
     *      If the binding ID is not understood.
     */
    public static @NotNull BindingID parse(String lexical) {
        if(lexical.equals(XML_HTTP.toString()))
            return XML_HTTP;
        if(belongsTo(lexical,SOAP11_HTTP.toString()))
            return customize(lexical,SOAP11_HTTP);
        if(belongsTo(lexical,SOAP12_HTTP.toString()))
            return customize(lexical,SOAP12_HTTP);


        // OK, it's none of the values JAX-WS understands.
        for( BindingIDFactory f : ServiceFinder.find(BindingIDFactory.class) ) {
            BindingID r = f.parse(lexical);
            if(r!=null)
                return r;
        }

        // nobody understood this value
        throw new WebServiceException("Wrong binding ID: "+lexical);
    }

    private static boolean belongsTo(String lexical, String id) {
        return lexical.equals(id) || lexical.startsWith(id+'?');
    }

    /**
     * Parses parameter portion and returns appropriately populated {@link SOAPHTTPImpl}
     */
    private static SOAPHTTPImpl customize(String lexical, SOAPHTTPImpl base) {
        if(lexical.equals(base.toString()))
            return base;

        // otherwise we must have query parameter
        // we assume the spec won't define any tricky parameters that require
        // complicated handling (such as %HH or non-ASCII char), so this parser
        // is quite simple-minded.
        SOAPHTTPImpl r = new SOAPHTTPImpl(base.getSOAPVersion(), lexical, base.canGenerateWSDL());
        try {
            String query = URLDecoder.decode(lexical.substring(lexical.indexOf('?')+1),"UTF-8");
            for( String token : query.split("&") ) {
                int idx = token.indexOf('=');
                if(idx<0)
                    throw new WebServiceException("Malformed binding ID (no '=' in "+token+")");
                r.parameters.put(token.substring(0,idx),token.substring(idx+1));
            }
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);    // UTF-8 is supported everywhere
        }

        return r;
    }


    /**
     * Figures out the binding from {@link BindingType} annotation.
     *
     * @return
     *      default to {@link BindingID#SOAP11_HTTP}, if no such annotation is present.
     * @see #parse(String)
     */
    public static @NotNull BindingID parse(Class<?> implClass) {
        BindingType bindingType = implClass.getAnnotation(BindingType.class);
        if (bindingType != null) {
            String bindingId = bindingType.value();
            if (bindingId.length() > 0) {
                return BindingID.parse(bindingId);
            }
        }
        return SOAP11_HTTP;
    }

    /**
     * Constant that represents SOAP1.2/HTTP.
     */
    public static final SOAPHTTPImpl SOAP12_HTTP = new SOAPHTTPImpl(
        SOAPVersion.SOAP_12, SOAPBinding.SOAP12HTTP_BINDING, false);
    /**
     * Constant that represents SOAP1.1/HTTP.
     */
    public static final SOAPHTTPImpl SOAP11_HTTP = new SOAPHTTPImpl(
        SOAPVersion.SOAP_11, SOAPBinding.SOAP11HTTP_BINDING, true);

    /**
     * Constant that represents REST.
     */
    public static final BindingID XML_HTTP = new Impl(null, HTTPBinding.HTTP_BINDING,false) {
        public @NotNull Encoder createEncoder(WSBinding _) {
            return TestEncoderImpl.INSTANCE11;
        }

        public @NotNull Decoder createDecoder(WSBinding _) {
            return TestDecoderImpl.INSTANCE11;
        }
    };

    private static abstract class Impl extends BindingID {
        final SOAPVersion version;
        private final String lexical;
        private final boolean canGenerateWSDL;

        public Impl(SOAPVersion version, String lexical, boolean canGenerateWSDL) {
            this.version = version;
            this.lexical = lexical;
            this.canGenerateWSDL = canGenerateWSDL;
        }

        public SOAPVersion getSOAPVersion() {
            return version;
        }

        public String toString() {
            return lexical;
        }

        @Deprecated
        public boolean canGenerateWSDL() {
            return canGenerateWSDL;
        }
    }

    /**
     * Internal implementation for SOAP/HTTP.
     */
    private static final class SOAPHTTPImpl extends Impl implements Cloneable {
        /*final*/ Map<String,String> parameters = new HashMap<String,String>();

        public SOAPHTTPImpl(SOAPVersion version, String lexical, boolean canGenerateWSDL) {
            super(version, lexical, canGenerateWSDL);
        }

        public @NotNull Encoder createEncoder(WSBinding binding) {
            return new EncoderFacade(binding);
        }

        public @NotNull Decoder createDecoder(WSBinding binding) {
            return new DecoderFacade(version);
        }

        public boolean isMTOMEnabled() {
            return getParameter("mtom","false").equals("true");
        }
    }
}
