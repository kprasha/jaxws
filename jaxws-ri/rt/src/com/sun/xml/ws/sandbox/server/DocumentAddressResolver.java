/*
 * DocumentAddressResolver.java
 *
 * Created on February 6, 2006, 4:35 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.sun.xml.ws.sandbox.server;

/**
 *
 * @author Kohsuke Kawaguchi
 */
public interface DocumentAddressResolver {
    String getRelativeAddressFor(SDDocument current, SDDocument referenced);
}
