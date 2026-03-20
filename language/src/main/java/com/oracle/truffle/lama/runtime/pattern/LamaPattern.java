package com.oracle.truffle.lama.runtime.pattern;

import java.util.Map;

public interface LamaPattern {
    /**
     * @return bindings if pattern matched, otherwise {@code null}.
     */
    Map<String, Object> match(Object value);
}

