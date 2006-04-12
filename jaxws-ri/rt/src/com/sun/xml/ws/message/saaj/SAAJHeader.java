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
package com.sun.xml.ws.message.saaj;

import com.sun.xml.ws.api.message.Header;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.message.DOMHeader;
import com.sun.istack.NotNull;

import javax.xml.soap.SOAPHeaderElement;

/**
 * {@link Header} for {@link SOAPHeaderElement}.
 *
 * @author Vivek Pandey
 */
public final class SAAJHeader extends DOMHeader<SOAPHeaderElement> {

    private boolean isMustUnderstood;
    private String role;
    private boolean relay;
    private int flags;

    private static final int FLAG_ACTOR            = 0x0001;
    private static final int FLAG_MUST_UNDERSTAND   = 0x0002;
    private static final int FLAG_RELAY             = 0x0004;


    public SAAJHeader(SOAPHeaderElement header) {
        // we won't rely on any of the super class method that uses SOAPVersion,
        // so we can just pass in a dummy version
        super(header);
    }

    /**
     * True if this header must be understood.
     *
     * Read the mustUndestandHeader only once, save reading it from DOM everytime.
     */
    public boolean isMustUnderstood() {
        if(isSet(FLAG_MUST_UNDERSTAND))
            return isMustUnderstood;

        isMustUnderstood = node.getMustUnderstand();
        set(FLAG_MUST_UNDERSTAND);
        return isMustUnderstood;
    }

    /**
     * Gets the value of the soap:role attribute (or soap:actor for SOAP 1.1).
     * <p/>
     * <p/>
     * SOAP 1.1 values are normalized into SOAP 1.2 values.
     * <p/>
     * An omitted SOAP 1.1 actor attribute value will become:
     * "http://www.w3.org/2003/05/soap-envelope/role/ultimateReceiver"
     * An SOAP 1.1 actor attribute value of:
     * "http://schemas.xmlsoap.org/soap/actor/next"
     * will become:
     * "http://www.w3.org/2003/05/soap-envelope/role/next"
     * <p/>
     * <p/>
     * If the soap:role attribute is absent, this method returns
     * "http://www.w3.org/2003/05/soap-envelope/role/ultimateReceiver".
     *
     * @return never null. This string need not be interned.
     */
    @Override
    public @NotNull String getRole(SOAPVersion soapVersion) {
        if(isSet(FLAG_ACTOR))
            return role;

        role = node.getActor();

        //SAAJ may return null, lets return the default value in that case
        //TODO: findout SOAP version
        if(role == null)
            role = "http://schemas.xmlsoap.org/soap/actor/next";

        set(FLAG_ACTOR);
        return role;
    }

    /**
     * True if this header is to be relayed if not processed.
     * For SOAP 1.1 messages, this method always return false.
     * <p/>
     * <p/>
     * IOW, this method returns true if there's @soap:relay='true'
     * is present.
     * <p/>
     * <h3>Implementation Note</h3>
     * <p/>
     * The implementation needs to check for both "true" and "1",
     * but because attribute values are normalized, it doesn't have
     * to consider " true", " 1 ", and so on.
     *
     * @return false.
     */
    public boolean isRelay() {
        if(isSet(FLAG_RELAY))
            return relay;

        //SAAJ throws UnsupportedOperationException if its SOAP 1.1 version
        //Ideally this method should always throw false for SOAP 1.1
        try{
            relay = node.getRelay();
        }catch(UnsupportedOperationException e){
            relay = false;
        }
        set(FLAG_RELAY);
        return relay;
    }

    protected boolean isSet(int flag){
        return (flags&flag) != 0;
    }

    protected void set(int flag){
        flags |= flag;
    }
}
