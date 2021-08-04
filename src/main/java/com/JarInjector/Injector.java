package com.JarInjector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Injector {
    private static final String BASE_CLASS_PREFIX = "Base__";
    private static final String FULL_NAME_DELIMITER = "/";
    private static final String INTERMEDIATE_JAR_FILE_NAME_SUFFIX = "-tmp";
    private static final String OUT_JAR_FILE_NAME_SUFFIX = "-new";

    private final String originalJarFile;
    private final String[] javaFiles;
    private String intermediateJarFile;
    private String outJarFile;
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

    public Injector(String jarFile, String[] javaFiles) {
        this.originalJarFile = jarFile;
        this.javaFiles = javaFiles;
        initJarNames();
    }

    private void initJarNames() {
        File file = new File(originalJarFile);
        String path = file.getParent();
        String nameWithExtension = file.getName();
        String nameWithoutExtension = nameWithExtension.substring(0, nameWithExtension.length() - ".jar".length());
        intermediateJarFile = Paths.get(path, nameWithoutExtension + INTERMEDIATE_JAR_FILE_NAME_SUFFIX + ".jar").toString();
        outJarFile = Paths.get(path, nameWithoutExtension + OUT_JAR_FILE_NAME_SUFFIX + ".jar").toString();
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
        JarModifier jarModifier = new JarModifier(originalJarFile);

        for (String entry : entriesToReplace) {
            byte[] bytes = jarModifier.readEntry(entry);
            EntryName entryName = buildBase__Name(entry);

            ClassModifier classModifier = new ClassModifier(bytes);
            classModifier.renameClass(entryName.shortName);

            jarModifier
                    .deleteEntry(entry)
                    .putEntry(entryName.fullName, classModifier.getBytes());
        }

        jarModifier.build(intermediateJarFile);
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
        List<String> command = new ArrayList<>();
        command.add("javac");
        command.add("-classpath");
        command.add(intermediateJarFile);
        command.addAll(Arrays.asList(javaFiles));
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        process.waitFor();
    }

    private void putCompiledClassesIntoJar() throws IOException {
        JarModifier jarModifier = new JarModifier(intermediateJarFile);

        for (Map.Entry<String, String> mapEntry : entryNamePerJavaClass.entrySet()) {
            String entryName = mapEntry.getValue();
            String javaFileName = mapEntry.getKey();
            int index = javaFileName.lastIndexOf(".");
            String classFileName = javaFileName.substring(0, index) + ".class";
            jarModifier.putEntry(entryName, Files.readAllBytes(Paths.get(classFileName)));
        }

        jarModifier.build(outJarFile);
    }
}
