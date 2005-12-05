package com.sun.xml.ws.sandbox.chain;

/**
 * @author Kohsuke Kawaguchi
 */
public interface ChainFactory {
    // TODO: think about what would be the input
    Chain create( Object/* TODO: change it to Model, or maybe a new 'Policy' */ model );
}
