package org.teavm.classlib.java.security;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class TDigestInputStream extends FilterInputStream {
    protected TMessageDigest digest;

    public TDigestInputStream(InputStream stream, TMessageDigest digest) {
        super(stream);
        this.digest = digest;
    }

    @Override
    public int read() throws IOException {
        int b = in.read();
        if (b >= 0) {
            digest.update((byte) b);
        }
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int n = in.read(b, off, len);
        if (n > 0) {
            digest.update(b, off, n);
        }
        return n;
    }

    public TMessageDigest getMessageDigest() {
        return digest;
    }
}
