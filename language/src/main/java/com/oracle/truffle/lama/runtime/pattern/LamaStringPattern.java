package com.oracle.truffle.lama.runtime.pattern;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

public final class LamaStringPattern implements LamaPattern {
    private final byte[] expected;

    public LamaStringPattern(String expected) {
        this.expected = expected.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public Map<String, Object> match(Object value) {
        Object v = LamaPatternUtil.normalize(value);
        if (v instanceof byte[] bytes && Arrays.equals(bytes, expected)) {
            return Map.of();
        }
        return null;
    }
}

