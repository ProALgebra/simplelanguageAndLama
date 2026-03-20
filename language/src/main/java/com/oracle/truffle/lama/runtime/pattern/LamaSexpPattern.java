package com.oracle.truffle.lama.runtime.pattern;

import com.oracle.truffle.lama.runtime.LamaSexp;
import java.util.HashMap;
import java.util.Map;

public final class LamaSexpPattern implements LamaPattern {
    private final String tag;
    private final LamaPattern[] fields;

    public LamaSexpPattern(String tag, LamaPattern[] fields) {
        this.tag = tag;
        this.fields = fields;
    }

    @Override
    public Map<String, Object> match(Object value) {
        if (!(value instanceof LamaSexp sexp)) {
            return null;
        }
        if (!tag.equals(sexp.tag())) {
            return null;
        }
        if (sexp.length() != fields.length) {
            return null;
        }
        Map<String, Object> bindings = new HashMap<>();
        for (int i = 0; i < fields.length; i++) {
            Object fieldValue = LamaPatternUtil.normalize(sexp.get(i));
            Map<String, Object> b = fields[i].match(fieldValue);
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
