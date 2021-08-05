package com.JarInjector;

import org.apache.commons.cli.*;

import java.io.IOException;

public class Main {
    private static class InjectorOptions {
        String jarFile;
        String[] javaSources;
        String[] jarsToCompileWith;
    }

    public static void main(String[] args) {
        InjectorOptions injectorOptions = null;
        try {
            injectorOptions = parseArgs(args);
        } catch (ParseException ignored) {
        }

        if (injectorOptions != null) {
            Injector injector = new Injector(injectorOptions.jarFile, injectorOptions.javaSources);
            try {
                injector.inject();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static InjectorOptions parseArgs(String[] args) throws ParseException {
        InjectorOptions injectorOptions = new InjectorOptions();
        Options options = new Options();
        options.addOption(Option.builder("jar")
                .hasArg()
                .required()
                .desc("Jar file to be modified")
                .build());

        options.addOption(Option.builder("src")
                .hasArgs()
                .required()
                .desc("Java source files to inject into jar")
                .build());

        options.addOption(Option.builder("cw")
                .hasArgs()
                .required(false)
                .desc("Additional jars to compile with")
                .build());

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine line = parser.parse(options, args);
            injectorOptions.jarFile = line.getOptionValue("jar");
            injectorOptions.javaSources = line.getOptionValues("src");
            injectorOptions.jarsToCompileWith = line.getOptionValues("cw");
            return injectorOptions;
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("JarInjector", options);
            throw e;
        }
    }
}
