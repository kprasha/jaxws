package com.sun.xml.ws.transport.local;

import com.sun.xml.ws.transport.http.ResourceLoader;

import java.io.File;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Set;
import java.util.HashSet;

/**
 * {@link ResourceLoader} that deals with the expanded image of a war file
 * on a file system.
 */
public final class FileSystemResourceLoader implements ResourceLoader {
    /**
     * The root of the exploded war file.
     */
    private final File root;

    public FileSystemResourceLoader(File root) {
        this.root = root;
    }

    public URL getResource(String path) throws MalformedURLException {
        return new File(root+path).toURL();
    }

    public URL getCatalogFile() throws MalformedURLException {
        return getResource("/WEB-INF/jax-ws-catalog.xml");
    }

    public Set<String> getResourcePaths(String path) {
        Set<String> r = new HashSet<String>();
        for( File f : new File(root+path).listFiles() ) {
            if(f.isDirectory()) {
                r.add(path+f.getName()+'/');
            } else {
                r.add(path+f.getName());
            }
        }
        return r;
    }
}
