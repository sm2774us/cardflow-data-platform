package com.cardflow.platform.lake;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class LocalLakeSink implements LakeSink {
    private final Path root;

    public LocalLakeSink(String dir) { this.root = Path.of(dir); }

    @Override
    public String write(String key, byte[] data) throws IOException {
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root.normalize())) throw new IOException("Illegal key " + key);
        Files.createDirectories(target.getParent());
        Files.write(target, data);
        return target.toUri().toString();
    }
}
