package com.sun.xml.ws.sandbox.message.impl;

import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.api.message.Header;

import javax.xml.soap.SOAPConstants;
import javax.xml.namespace.QName;

/**
 * Partial default implementation of {@link Header}.
 *
 * <p>
 * This is meant to be a convenient base class
 * for {@link Header}-derived classes.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class AbstractHeaderImpl implements Header {

    protected final SOAPVersion soapVersion;

    protected AbstractHeaderImpl(SOAPVersion soapVersion) {
        this.soapVersion = soapVersion;
    }

    public boolean isMustUnderstood() {
        String v = getAttribute(soapVersion.nsUri, "mustUnderstand");
        if(v==null) return false;
        return parseBool(v);
    }

    public String getRole() {
        String v;

        switch(soapVersion) {
        case SOAP_11:
            v = getAttribute(soapVersion.nsUri,"actor");
            if(v==null)
                return SOAPConstants.URI_SOAP_1_2_ROLE_ULTIMATE_RECEIVER;
            if(v.equals(SOAPConstants.URI_SOAP_ACTOR_NEXT))
                return SOAPConstants.URI_SOAP_1_2_ROLE_NEXT;
            return v;
        case SOAP_12:
            v = getAttribute(soapVersion.nsUri,"role");
            if(v==null)
                return SOAPConstants.URI_SOAP_1_2_ROLE_ULTIMATE_RECEIVER;
            return v;
        default:
            throw new AssertionError();
        }
    }

    public boolean isRelay() {
        String v = getAttribute(soapVersion.nsUri,"relay");
        if(v==null) return false;   // on SOAP 1.1 message there shouldn't be such an attribute, so this works fine
        return parseBool(v);
    }

    public String getAttribute(QName name) {
        return getAttribute(name.getNamespaceURI(),name.getLocalPart());
    }

    /**
     * Parses a string that looks like <tt>xs:boolean</tt> into boolean.
     *
     * This method assumes that the whilespace normalization has already taken place.
     */
    protected final boolean parseBool(String value) {
        if(value.length()==0)
            return false;

        char ch = value.charAt(0);
        return ch=='t' || ch=='1';
    }
}