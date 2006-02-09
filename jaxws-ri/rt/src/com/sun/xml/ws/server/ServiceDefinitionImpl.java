package com.sun.xml.ws.server;

import com.sun.xml.ws.sandbox.server.SDDocument;
import com.sun.xml.ws.sandbox.server.ServiceDefinition;

import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * {@link ServiceDefinition} implementation.
 *
 * <p>
 * You construct a {@link ServiceDefinitionImpl} by first constructing
 * a list of {@link SDDocumentImpl}s.
 *
 * @author Kohsuke Kawaguchi
 */
public final class ServiceDefinitionImpl implements ServiceDefinition {
    private final List<SDDocumentImpl> docs;

    private final Map<String, SDDocumentImpl> bySystemId;

    /**
     * Set when {@link WSEndpointImpl} is created.
     */
    /*package*/ WSEndpointImpl<?> owner;

    /**
     * @param docs
     *      List of {@link SDDocumentImpl}s to form the description.
     *      There must be at least one entry.
     *      The first document is considered {@link #getPrimary() primary}.
     */
    public ServiceDefinitionImpl(List<SDDocumentImpl> docs) {
        assert !docs.isEmpty();
        this.docs = docs;

        this.bySystemId = new HashMap<String, SDDocumentImpl>(docs.size());
        for (SDDocumentImpl doc : docs) {
            bySystemId.put(doc.getURL().toExternalForm(),doc);

            assert doc.owner==null;
            doc.owner = this;
        }
    }

    /**
     * The owner is set when {@link WSEndpointImpl} is created.
     */
    /*package*/ void setOwner(WSEndpointImpl<?> owner) {
        assert owner!=null && this.owner==null;
        this.owner = owner;
    }

    public SDDocument getPrimary() {
        return docs.get(0);
    }

    public Iterator<SDDocument> iterator() {
        return (Iterator)docs.iterator();
    }

    /**
     * @see #getBySystemId(String)
     */
    public SDDocument getBySystemId(URL systemId) {
        return getBySystemId(systemId.toString());
    }

    /**
     * Gets the {@link SDDocumentImpl} whose {@link SDDocumentImpl#getURL()}
     * returns the specified value.
     *
     * @return
     *      null if none is found.
     */
    public SDDocumentImpl getBySystemId(String systemId) {
        return bySystemId.get(systemId);
    }
}
