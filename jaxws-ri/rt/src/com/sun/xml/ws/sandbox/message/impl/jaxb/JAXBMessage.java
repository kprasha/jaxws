package com.sun.xml.ws.sandbox.message.impl.jaxb;

import com.sun.xml.ws.sandbox.message.Message;
import com.sun.xml.ws.sandbox.message.HeaderList;
import com.sun.xml.ws.sandbox.message.MessageProperties;
import com.sun.xml.ws.sandbox.message.AttachmentSet;

/**
 * {@link Message} backed by a JAXB bean.
 *
 * @author Kohsuke Kawaguchi
 */
public /*for now, work in progress*/ abstract class JAXBMessage extends Message {
    private HeaderList headers;
    private final MessageProperties props;

    public JAXBMessage() {
        props = new MessageProperties();
    }

    /**
     * Copy constructor.
     */
    public JAXBMessage(JAXBMessage that) {
        this.headers = that.headers;
        if(this.headers!=null)
            this.headers = new HeaderList(this.headers);
        // TODO: do we need to clone this? I guess so.
        this.props = that.props;
    }

    public boolean hasHeaders() {
        return (headers == null) ? false : headers.size() > 0;
    }
    
    public HeaderList getHeaders() {
        if(headers==null)
            headers = new HeaderList();
        return headers;
    }

    public MessageProperties getProperties() {
        return props;
    }

    public Message copy() {
        return new JAXBMessage(this);
    }


}
