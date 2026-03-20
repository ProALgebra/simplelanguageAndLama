package com.oracle.truffle.lama.runtime.pattern;

import com.oracle.truffle.lama.runtime.LamaCallable;
import com.oracle.truffle.lama.runtime.LamaSexp;
import java.util.Map;

public final class LamaTypePattern implements LamaPattern {
    private final String kind;

    public LamaTypePattern(String kind) {
        this.kind = kind;
    }

    @Override
    public Map<String, Object> match(Object value) {
        Object v = LamaPatternUtil.normalize(value);
        boolean ok = switch (kind) {
            case "val" -> v instanceof Number;
            case "str" -> v instanceof byte[];
            case "array" -> v instanceof Object[];
            case "sexp" -> v instanceof LamaSexp;
            case "fun" -> v instanceof LamaCallable;
            default -> false;
        };
        return ok ? Map.of() : null;
    }
}

