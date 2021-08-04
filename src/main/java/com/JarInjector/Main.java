package com.JarInjector;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: injector jarFile javaFile1 [JavaFile2 ...]");
            return;
        }

        String jarFile = args[0];
        String[] javaSources = new String[args.length - 1];
        System.arraycopy(args, 1, javaSources, 0, args.length - 1);
        Injector injector = new Injector(jarFile, javaSources);
        try {
            injector.inject();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
