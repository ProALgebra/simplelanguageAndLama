package com.oracle.truffle.lama.runtime.pattern;

import java.util.Map;

public final class LamaLongPattern implements LamaPattern {
    private final long expected;

    public LamaLongPattern(long expected) {
        this.expected = expected;
    }

    @Override
    public Map<String, Object> match(Object value) {
        Object v = LamaPatternUtil.normalize(value);
        if (v instanceof Number n && n.longValue() == expected) {
            return Map.of();
        }
        return null;
    }
}

