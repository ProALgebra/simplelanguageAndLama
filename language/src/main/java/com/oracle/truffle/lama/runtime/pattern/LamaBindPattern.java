package com.oracle.truffle.lama.runtime.pattern;

import java.util.Map;

public final class LamaBindPattern implements LamaPattern {
    private final String name;

    public LamaBindPattern(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public Map<String, Object> match(Object value) {
        return Map.of(name, LamaPatternUtil.normalize(value));
    }
}
