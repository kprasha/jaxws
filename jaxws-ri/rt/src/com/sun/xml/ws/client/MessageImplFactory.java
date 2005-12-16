/*
 * Copyright (c) 2005 Sun Microsystems. All Rights Reserved.
 */
package com.sun.xml.ws.client;

import com.sun.xml.ws.sandbox.message.Message;
import com.sun.xml.ws.sandbox.message.impl.saaj.SAAJMessage;

import javax.xml.soap.SOAPMessage;
import javax.xml.ws.WebServiceException;

public class MessageImplFactory {
    private static MessageImplFactory instance = new MessageImplFactory();

    public static MessageImplFactory getInstance() {
        return instance;
    }

    private MessageImplFactory() {
    }

    public static Message createMessage(Object msg){
        if (msg instanceof SOAPMessage)
           return new SAAJMessage((SOAPMessage)msg);
        else throw new WebServiceException("Unknown message type");
    }
}
