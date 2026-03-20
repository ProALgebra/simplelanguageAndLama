package com.oracle.truffle.lama.runtime.pattern;

import java.util.HashMap;
import java.util.Map;

public final class LamaAsPattern implements LamaPattern {
    private final String name;
    private final LamaPattern inner;

    public LamaAsPattern(String name, LamaPattern inner) {
        this.name = name;
        this.inner = inner;
    }

    @Override
    public Map<String, Object> match(Object value) {
        Map<String, Object> innerBindings = inner.match(value);
        if (innerBindings == null) {
            return null;
        }
        Map<String, Object> bindings = new HashMap<>(innerBindings);
        Object normalized = LamaPatternUtil.normalize(value);
        Object existing = bindings.putIfAbsent(name, normalized);
        if (existing != null && !LamaPatternUtil.deepEquals(existing, normalized)) {
            return null;
        }
        return bindings;
    }
}

