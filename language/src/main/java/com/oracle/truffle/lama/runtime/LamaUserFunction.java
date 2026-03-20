package com.oracle.truffle.lama.runtime;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.lama.LamaContext;
import com.oracle.truffle.lama.nodes.LamaExpressionNode;
import com.oracle.truffle.lama.runtime.pattern.LamaPattern;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.oracle.truffle.lama.runtime.pattern.LamaBindPattern;
import com.oracle.truffle.lama.runtime.pattern.LamaWildcardPattern;

public final class LamaUserFunction implements LamaCallable {
    private final List<LamaPattern> parameters;
    private final LamaExpressionNode body;
    private final LamaEnv capturedEnv;

    public LamaUserFunction(List<LamaPattern> parameters, LamaExpressionNode body, LamaEnv capturedEnv) {
        this.parameters = parameters;
        this.body = body;
        this.capturedEnv = capturedEnv;
    }

    @Override
    public Object call(LamaContext context, VirtualFrame frame, Object[] args) {
        context.pushEnvWithParent(capturedEnv);
        try {
            if (args.length != parameters.size()) {
                throw new RuntimeException("Arity mismatch: expected " + parameters.size() + ", got " + args.length);
            }

            Map<String, Object> bindings = null;
            for (int i = 0; i < parameters.size(); i++) {
                LamaPattern pattern = parameters.get(i);
                if (pattern instanceof LamaWildcardPattern) {
                    continue;
                }
                if (pattern instanceof LamaBindPattern bind) {
                    if (bindings == null) {
                        bindings = new HashMap<>();
                    }
                    Object value = args[i] == null ? 0L : args[i];
                    Object existing = bindings.putIfAbsent(bind.getName(), value);
                    if (existing != null && !sameValue(existing, value)) {
                        throw new RuntimeException("Conflicting bindings for '" + bind.getName() + "'");
                    }
                    continue;
                }

                Map<String, Object> b = pattern.match(args[i]);
                if (b == null) {
                    throw new RuntimeException("Pattern mismatch in function call");
                }
                if (bindings == null) {
                    bindings = new HashMap<>();
                }
                for (var e : b.entrySet()) {
                    Object existing = bindings.putIfAbsent(e.getKey(), e.getValue());
                    if (existing != null && !sameValue(existing, e.getValue())) {
                        throw new RuntimeException("Conflicting bindings for '" + e.getKey() + "'");
                    }
                }
            }

            if (bindings != null) {
                for (var entry : bindings.entrySet()) {
                    Object v = entry.getValue();
                    context.declareVar(entry.getKey(), v == null ? 0L : v);
                }
            }

            return body.executeGeneric(frame);
        } finally {
            context.popEnv();
        }
    }

    private static boolean sameValue(Object a, Object b) {
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
}
