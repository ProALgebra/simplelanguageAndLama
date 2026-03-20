package com.oracle.truffle.lama.runtime.pattern;

import java.util.HashMap;
import java.util.Map;

public final class LamaArrayPattern implements LamaPattern {
    private final LamaPattern[] elements;

    public LamaArrayPattern(LamaPattern[] elements) {
        this.elements = elements;
    }

    @Override
    public Map<String, Object> match(Object value) {
        Object v = LamaPatternUtil.normalize(value);
        if (!(v instanceof Object[] arr)) {
            return null;
        }
        if (arr.length != elements.length) {
            return null;
        }
        Map<String, Object> bindings = new HashMap<>();
        for (int i = 0; i < elements.length; i++) {
            Object elem = LamaPatternUtil.normalize(arr[i]);
            Map<String, Object> b = elements[i].match(elem);
            if (b == null) {
                return null;
            }
            if (!LamaPatternUtil.mergeInto(bindings, b)) {
                return null;
            }
        }
        return bindings;
    }
}

