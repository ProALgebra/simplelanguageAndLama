package com.oracle.truffle.lama.test;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.junit.Test;

public class LamaTest074 {
    @Test
    public void runsTest074() throws IOException {
        Path regressionDir = Path.of("hw2", "regression");
        if (!Files.isDirectory(regressionDir)) {
            regressionDir = Path.of("..", "hw2", "regression");
        }

        Path lamaFile = regressionDir.resolve("test074.lama");
        Path inputFile = regressionDir.resolve("test074.input");
        Path expectedFile = regressionDir.resolve("orig").resolve("test074.log");

        byte[] inputBytes = Files.readAllBytes(inputFile);
        String expected = Files.readString(expectedFile, StandardCharsets.UTF_8);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream(inputBytes);

        try (Context context = Context.newBuilder("lama").in(in).out(out).build()) {
            Source source = Source.newBuilder("lama", lamaFile.toFile()).build();
            context.eval(source);
        }

        String actual = out.toString(StandardCharsets.UTF_8);
        assertEquals(expected, actual);
    }
}

