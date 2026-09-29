package com.cardflow.platform.lake;

import java.io.IOException;

public interface LakeSink {
    /** Writes an object and returns its location URI. Must be idempotent for the same key. */
    String write(String key, byte[] data) throws IOException;
}
