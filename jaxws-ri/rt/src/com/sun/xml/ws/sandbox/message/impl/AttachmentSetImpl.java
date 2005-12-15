package com.sun.xml.ws.sandbox.message.impl;

import com.sun.xml.ws.sandbox.message.AttachmentSet;
import com.sun.xml.ws.sandbox.message.Attachment;

import java.util.ArrayList;

/**
 * Default dumb {@link AttachmentSet} implementation backed by {@link ArrayList}.
 *
 * <p>
 * The assumption here is that the number of attachments are small enough to
 * justify linear search in {@link #get(String)}.
 *
 * @author Kohsuke Kawaguchi
 */
public final class AttachmentSetImpl extends ArrayList<Attachment> implements AttachmentSet {

    /**
     * Creates an empty {@link AttachmentSet}.
     */
    public AttachmentSetImpl() {
    }

    /**
     * Creates an {@link AttachmentSet} by copying contents from another.
     */
    public AttachmentSetImpl(Iterable<Attachment> base) {
        for (Attachment a : base)
            add(a);
    }

    public Attachment get(String contentId) {
        for( int i=size()-1; i>=0; i-- ) {
            Attachment a = super.get(i);
            if(a.getContentId().equals(contentId))
                return a;
        }
        return null;
    }


}
