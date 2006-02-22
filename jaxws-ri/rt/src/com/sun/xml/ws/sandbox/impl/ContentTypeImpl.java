package com.sun.xml.ws.sandbox.impl;

import com.sun.xml.ws.api.pipe.ContentType;

/**
 * @author Vivek Pandey
 */
class ContentTypeImpl implements ContentType {
    private final String contentType;
    private final String soapAction;

    public ContentTypeImpl(String contentType, String soapAction) {
        this.contentType = contentType;
        this.soapAction = soapAction;
    }

    public String getContentType() {
        return contentType;
    }

    public String getSOAPAction() {
        return soapAction;
    }
}
