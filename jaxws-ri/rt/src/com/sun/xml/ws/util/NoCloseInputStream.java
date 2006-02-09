package com.sun.xml.ws.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * {@link InputStream} that cannot be closed.
 *
 * @author Kohsuke Kawaguchi
 */
public class NoCloseInputStream extends FilterInputStream {
    public NoCloseInputStream(InputStream is) {
        super(is);
    }

    @Override
    public void close() throws IOException {
        // Intentionally left empty. use closeInput() to close
    }

    public void doClose() throws IOException {
        super.close();
    }
}
