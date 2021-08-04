package com.JarInjector;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public class JarModifier {
    private static class Entry {
        Entry(String name, byte[] bytes) {
            this.name = name;
            this.bytes = bytes;
        }

        String name;
        byte[] bytes;
    }

    private final JarFile jarFile;
    private final List<String> entriesToDelete = new ArrayList<>();
    private final List<Entry> entriesToPut = new ArrayList<>();

    public JarModifier(String path) throws IOException {
        jarFile = new JarFile(path);
    }

    public byte[] readEntry(String name) throws IOException {
        JarEntry jarEntry = jarFile.getJarEntry(name);
        InputStream inputStream = jarFile.getInputStream(jarEntry);
        byte[] bytes = new byte[inputStream.available()];
        inputStream.read(bytes);
        inputStream.close();
        return bytes;
    }

    public JarModifier deleteEntry(String name) {
        entriesToDelete.add(name);
        return this;
    }

    public JarModifier putEntry(String name, byte[] bytes) {
        entriesToDelete.add(name);
        entriesToPut.add(new Entry(name, bytes));
        return this;
    }

    public void build(String path) throws IOException {
        JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(path));

        Enumeration<JarEntry> jarEntryEnumeration = jarFile.entries();
        while (jarEntryEnumeration.hasMoreElements()) {
            final JarEntry jarEntry = jarEntryEnumeration.nextElement();
            if (!entriesToDelete.contains(jarEntry.getName())) {
                jarOutputStream.putNextEntry(new JarEntry(jarEntry.getName()));
                jarOutputStream.write(readEntry(jarEntry.getName()));
                jarOutputStream.closeEntry();
            }
        }

        for (Entry entry : entriesToPut) {
            jarOutputStream.putNextEntry(new JarEntry(entry.name));
            jarOutputStream.write(entry.bytes);
            jarOutputStream.closeEntry();
        }

        jarOutputStream.close();
    }
}
