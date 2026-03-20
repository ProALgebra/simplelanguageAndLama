package com.oracle.truffle.lama.test;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;

public final class LamaRegressionSupport {
    private LamaRegressionSupport() {
    }

    public static List<String> listCaseNames() throws IOException {
        Path regressionDir = regressionDir();
        List<String> names;
        try (var stream = Files.list(regressionDir)) {
            names = stream.filter(p -> p.getFileName().toString().matches("test\\d+\\.lama"))
                            .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                            .map(p -> p.getFileName().toString().replace(".lama", ""))
                            .collect(Collectors.toList());
        }
        String singleCase = System.getProperty("lama.regression.case");
        if (singleCase == null || singleCase.isBlank()) {
            return names;
        }
        if (!names.contains(singleCase)) {
            throw new IllegalArgumentException("Unknown regression case: " + singleCase);
        }
        return List.of(singleCase);
    }

    public static void runCase(String languageId, String base) throws IOException {
        Path regressionDir = regressionDir();
        Path origDir = regressionDir.resolve("orig");

        Path lamaFile = regressionDir.resolve(base + ".lama");
        Path inputFile = regressionDir.resolve(base + ".input");
        Path expectedFile = origDir.resolve(base + ".log");

        byte[] inputBytes = Files.readAllBytes(inputFile);
        String expected = Files.readString(expectedFile, StandardCharsets.UTF_8);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream(inputBytes);

        try (Context context = Context.newBuilder(languageId).in(in).out(out).build()) {
            Source source = Source.newBuilder(languageId, lamaFile.toFile()).build();
            try {
                context.eval(source);
            } catch (RuntimeException e) {
                throw new AssertionError("Failed to execute " + base + " (" + lamaFile + ")", e);
            }
        }

        String actual = out.toString(StandardCharsets.UTF_8);
        assertEquals("Mismatch in " + base, expected, actual);
    }

    private static Path regressionDir() {
        Path regressionDir = Path.of("hw2", "regression");
        if (!Files.isDirectory(regressionDir)) {
            regressionDir = Path.of("..", "hw2", "regression");
        }
        return regressionDir;
    }
}
