package com.oracle.truffle.lama.test;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.graalvm.polyglot.Context;
import org.junit.Test;

public class LamaSmokeTest {

    @Test
    public void writeArithmetic() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (Context context = Context.newBuilder("lama").out(out).build()) {
            context.eval("lama", "write(1 + 2 * 3);");
        }
        assertEquals("7\n", out.toString(StandardCharsets.UTF_8));
    }

    @Test
    public void readWrite() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream("41\n".getBytes(StandardCharsets.UTF_8));
        try (Context context = Context.newBuilder("lama").in(in).out(out).build()) {
            context.eval("lama", "write(read() + 1);");
        }
        assertEquals("> 42\n", out.toString(StandardCharsets.UTF_8));
    }
}
