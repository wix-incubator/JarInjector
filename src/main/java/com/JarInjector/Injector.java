package com.JarInjector;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Injector {
    private static final String BASE_CLASS_PREFIX = "Base__";
    private static final String FULL_NAME_DELIMITER = "/";

    private final String[] javaFiles;
    private final String[] jarsToCompileWith;
    private final String unpackedJarFile;
    private final List<String> entriesToReplace = new ArrayList<>();
    private final Map<String, String> entryNamePerJavaClass = new HashMap<>();

    private static class EntryName {
        String shortName;
        String fullName;

        EntryName(String shortName, String fullName) {
            this.shortName = shortName;
            this.fullName = fullName;
        }
    }

    public Injector(String jarFile, String[] javaFiles, String[] jarsToCompileWith) {
        this.unpackedJarFile = jarFile;
        this.javaFiles = javaFiles;
        this.jarsToCompileWith = jarsToCompileWith;
    }

    public void inject() throws IOException, InterruptedException {
        prepareEntries();
        replaceEntries();
        compileJavaFiles();
        putCompiledClassesIntoJar();
    }

    private void prepareEntries() throws IOException {
        for (String javaFile : javaFiles) {
            BufferedReader br = new BufferedReader(new FileReader(javaFile));
            String line;
            String entryName = "";
            while ((line = br.readLine()) != null) {
                String trimmedLine = line.trim();
                if (line.startsWith("package")) {
                    String packageName = extractPackageName((trimmedLine));
                    entryName = buildEntryName(packageName, javaFile);
                    entryNamePerJavaClass.put(javaFile, entryName);
                }
                if (line.contains(BASE_CLASS_PREFIX)) {
                    entriesToReplace.add(entryName);
                    break;
                }
            }
            br.close();
        }
    }

    private String extractPackageName(String packageLine) {
        return packageLine.substring("package".length(), packageLine.length() - 1).trim();
    }

    private String buildEntryName(String packageName, String classFileFullName) {
        packageName = packageName.replaceAll("\\.", FULL_NAME_DELIMITER);
        File file = new File(classFileFullName);
        String nameWithExtension = file.getName();
        String nameWithoutExtension = nameWithExtension.substring(0, nameWithExtension.length() - ".java".length());
        return packageName + "/" + nameWithoutExtension + ".class";
    }

    private void replaceEntries() throws IOException {
        UnpackedJarModifier jarModifier = new UnpackedJarModifier(unpackedJarFile);

        for (String entry : entriesToReplace) {
            replaceEntry(jarModifier, entry);
            // -------------- handle ...$... classes
            String entryPath = entry.substring(0, entry.lastIndexOf('/'));
            String startsWith = entry.substring(entryPath.length() + 1);
            startsWith = startsWith.substring(0, startsWith.length() - ".class".length()) + "$";
            String[] internalEntries = jarModifier.findClassEntries(entryPath, startsWith);
            for (String internalEntry : internalEntries) {
                replaceEntry(jarModifier, internalEntry);
            }
        }
    }

    private void replaceEntry(UnpackedJarModifier jarModifier, String entry) throws IOException {
        byte[] bytes = jarModifier.readEntry(entry);
        EntryName entryName = buildBase__Name(entry);
        ClassModifier classModifier = new ClassModifier(bytes);
        classModifier.renameClass(entryName.shortName);
        jarModifier.deleteEntry(entry);
        jarModifier.putEntry(entryName.fullName, classModifier.getBytes());
    }

    // "com/evgenis/simpleapp/logic/Logic.class"
    private EntryName buildBase__Name(String fullName) {
        String[] fullNameSegments = fullName.split(FULL_NAME_DELIMITER);
        String[] shortNameSegments = fullNameSegments[fullNameSegments.length - 1].split("\\.");
        String newShortName = BASE_CLASS_PREFIX + shortNameSegments[0];
        fullNameSegments[fullNameSegments.length - 1] = newShortName + ".class";
        return new EntryName(newShortName, String.join(FULL_NAME_DELIMITER, fullNameSegments));
    }

    private void compileJavaFiles() throws IOException, InterruptedException {
        String jars = "";
        if (jarsToCompileWith != null && jarsToCompileWith.length > 0) {
            jars = String.join(";", jarsToCompileWith);
        }
        if (jars.length() > 0) {
            jars = ":" + jars;
        }
        jars = unpackedJarFile + jars;
        List<String> command = new ArrayList<>();
        command.add("javac");
        command.add("-classpath");
        command.add(jars);
        command.addAll(Arrays.asList(javaFiles));
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        process.waitFor();
    }

    private void putCompiledClassesIntoJar() throws IOException {
        UnpackedJarModifier jarModifier = new UnpackedJarModifier(unpackedJarFile);

        for (Map.Entry<String, String> mapEntry : entryNamePerJavaClass.entrySet()) {
            String entryName = mapEntry.getValue();
            String javaFileName = mapEntry.getKey();
            int index = javaFileName.lastIndexOf(".");

            String classFileName = javaFileName.substring(0, index) + ".class";
            Path classPath = Paths.get(classFileName);
            jarModifier.putEntry(entryName, Files.readAllBytes(classPath));
            Files.delete(Paths.get(classFileName));

            // ---------- put ...$... class files into jar
            String entryPath = entryName.substring(0, entryName.lastIndexOf('/'));
            Path parent = classPath.getParent();
            String fileName = classPath.getFileName().toString();
            index = fileName.lastIndexOf(".");
            String fileNameWithoutExt = fileName.substring(0, index);
            File[] files = findClassFiles(parent.toFile(), fileNameWithoutExt + "$");
            for (File file : files) {
                jarModifier.putEntry(entryPath + "/" + file.getName(), Files.readAllBytes(file.toPath()));
                Files.delete(file.toPath());
            }
        }
    }

    private File[] findClassFiles(File dir, String startsWith) {
        return dir.listFiles((dir1, name) -> name.startsWith(startsWith) && name.endsWith(".class"));
    }

}
