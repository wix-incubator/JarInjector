package com.JarInjector;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class UnpackedJarModifier {
    private final File baseDir;

    public UnpackedJarModifier(String path) throws IOException {
        baseDir = new File(path);
    }

    public byte[] readEntry(String name) throws IOException {
        File file = new File(baseDir, name);
        return Files.readAllBytes(file.toPath());
    }

    public void deleteEntry(String name) {
        File file = new File(baseDir, name);
        file.delete();
    }

    public void putEntry(String name, byte[] bytes) throws IOException {
        File file = new File(baseDir, name);
        Files.write(file.toPath(), bytes, StandardOpenOption.CREATE);
    }

    public String[] findClassEntries(String entryPath, String startsWith) {
        File dir = new File(baseDir, entryPath);
        File[] files = dir.listFiles((dir1, name) -> name.startsWith(startsWith) && name.endsWith(".class"));
        if (files != null) {
            String[] result = new String[files.length];
            for (int i = 0; i < files.length; i++) {
                result[i] = entryPath + "/" + files[i].getName();
            }
            return result;
        } else {
            return null;
        }
    }

}
