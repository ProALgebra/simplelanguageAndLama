package com.oracle.truffle.lama.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.lama.LamaContext;
import com.oracle.truffle.lama.runtime.pattern.LamaPattern;
import java.util.Map;

public final class LamaCaseNode extends LamaExpressionNode {
    @Child private LamaExpressionNode scrutinee;
    private final LamaPattern[] patterns;
    @Children private final LamaExpressionNode[] bodies;

    public LamaCaseNode(LamaExpressionNode scrutinee, LamaPattern[] patterns, LamaExpressionNode[] bodies) {
        this.scrutinee = scrutinee;
        this.patterns = patterns;
        this.bodies = bodies;
        if (patterns.length != bodies.length) {
            throw new IllegalArgumentException("patterns and bodies must have same length");
        }
    }

    @Override
    @ExplodeLoop
    public Object executeGeneric(VirtualFrame frame) {
        Object value = scrutinee.executeGeneric(frame);
        LamaContext context = LamaContext.get(this);
        for (int i = 0; i < patterns.length; i++) {
            Map<String, Object> bindings = patterns[i].match(value);
            if (bindings == null) {
                continue;
            }
            context.pushEnv();
            try {
                for (var entry : bindings.entrySet()) {
                    Object v = entry.getValue();
                    context.declareVar(entry.getKey(), v == null ? 0L : v);
                }
                return bodies[i].executeGeneric(frame);
            } finally {
                context.popEnv();
            }
        }
        throw new RuntimeException("No matching case branch");
    }
}

