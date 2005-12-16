package com.sun.xml.ws.sandbox.message.impl;

import com.sun.xml.ws.sandbox.message.Message;

/**
 * Utility code for the {@link Message} implementation.
 */
public abstract class Util {
    /**
     * Parses a stringthat represents a boolean into boolean.
     * This method assumes that the whilespace normalization has already taken place.
     *
     * @param value
     */
    public static boolean parseBool(String value) {
        if(value.length()==0)
            return false;

        char ch = value.charAt(0);
        return ch=='t' || ch=='1';
    }

}
