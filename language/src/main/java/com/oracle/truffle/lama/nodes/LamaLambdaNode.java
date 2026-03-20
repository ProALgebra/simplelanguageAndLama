package com.oracle.truffle.lama.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.lama.LamaContext;
import com.oracle.truffle.lama.runtime.LamaUserFunction;
import com.oracle.truffle.lama.runtime.pattern.LamaPattern;
import java.util.List;

public final class LamaLambdaNode extends LamaExpressionNode {
    private final List<LamaPattern> parameters;
    @Child private LamaExpressionNode body;

    public LamaLambdaNode(List<LamaPattern> parameters, LamaExpressionNode body) {
        this.parameters = parameters;
        this.body = body;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        LamaContext context = LamaContext.get(this);
        return new LamaUserFunction(parameters, body, context.getCurrentEnv().snapshotForClosure());
    }
}
