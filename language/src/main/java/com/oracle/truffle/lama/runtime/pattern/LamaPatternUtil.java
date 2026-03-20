package com.oracle.truffle.lama.runtime.pattern;

import java.util.Arrays;
import java.util.Map;

final class LamaPatternUtil {
    private LamaPatternUtil() {
    }

    static Object normalize(Object value) {
        return value == null ? 0L : value;
    }

    static boolean deepEquals(Object a, Object b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a instanceof byte[] ab && b instanceof byte[] bb) {
            return Arrays.equals(ab, bb);
        }
        return a.equals(b);
    }

    static boolean mergeInto(Map<String, Object> target, Map<String, Object> other) {
        for (var e : other.entrySet()) {
            Object existing = target.get(e.getKey());
            if (existing != null) {
                if (!deepEquals(existing, e.getValue())) {
                    return false;
                }
            } else if (target.containsKey(e.getKey())) {
                if (!deepEquals(null, e.getValue())) {
                    return false;
                }
            } else {
                target.put(e.getKey(), e.getValue());
            }
        }
        return true;
    }
}

