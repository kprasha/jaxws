package com.sun.xml.ws.sandbox.pipe;

/**
 * @author Kohsuke Kawaguchi
 */
public interface PipeFactory {
    // TODO: think about what would be the input
    Pipe create( Object/* TODO: change it to Model, or maybe a new 'Policy' */ model );
}
