package com.sun.xml.ws.sandbox.message;

import java.util.Iterator;
import java.util.Collections;

/**
 * A set of {@link Attachment} on a {@link Message}.
 *
 * <p>
 * A particular attention is made to ensure that attachments
 * can be read and parsed lazily as requested.
 *
 * @see Message#getAttachments()
 */
public interface AttachmentSet extends Iterable<Attachment> {
    /**
     * Gets the attachment by the content ID.
     *
     * @return null
     *      if no such attachment exist.
     */
    Attachment get(String contentId);

    // adding attachment seems to be unnecessary --- true?
    // note that you can create a new AttachmentSet with existing
    // Attachments just fine. I'm talking about adding an Attachment
    // from a pipe or some such.

    ///**
    // * Adds an attachment to this set.
    // *
    // * <p>
    // * Note that it's OK for an {@link Attachment} to belong to
    // * more than one {@link AttachmentSet} (which is in fact
    // * necessary when you wrap a {@link Message} into another.
    // *
    // * @param att
    // *      must not be null.
    // */
    //void add(Attachment att);


    /**
     * Immutable {@link AttachmentSet} that has no {@link Attachment}.
     */
    // if we need mutation method on AttachmentSet, such singleton won't be possible.
    public static final AttachmentSet EMPTY = new AttachmentSet() {
        public Attachment get(String contentId) {
            return null;
        }

        public Iterator<Attachment> iterator() {
            return Collections.<Attachment>emptyList().iterator();
        }
    };
}
