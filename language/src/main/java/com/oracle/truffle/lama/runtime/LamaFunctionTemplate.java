package com.oracle.truffle.lama.runtime;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.lama.LamaContext;
import com.oracle.truffle.lama.nodes.LamaExpressionNode;
import com.oracle.truffle.lama.runtime.pattern.LamaPattern;
import java.util.List;

/**
 * A named function definition behaves like a function "template": each use as a value yields a fresh
 * closure that snapshots the current lexical environment (excluding globals).
 */
public final class LamaFunctionTemplate implements LamaCallable {
    private final List<LamaPattern> parameters;
    private final LamaExpressionNode body;
    private final LamaEnv definingEnv;

    public LamaFunctionTemplate(List<LamaPattern> parameters, LamaExpressionNode body, LamaEnv definingEnv) {
        this.parameters = parameters;
        this.body = body;
        this.definingEnv = definingEnv;
    }

    public LamaUserFunction instantiate() {
        return new LamaUserFunction(parameters, body, definingEnv.snapshotForClosure());
    }

    @Override
    public Object call(LamaContext context, VirtualFrame frame, Object[] args) {
        return instantiate().call(context, frame, args);
    }
}
