package com.oracle.truffle.lama.runtime.pattern;

import java.util.Map;

public final class LamaWildcardPattern implements LamaPattern {
    @Override
    public Map<String, Object> match(Object value) {
        return Map.of();
    }
}

