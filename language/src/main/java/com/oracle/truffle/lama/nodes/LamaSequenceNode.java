package com.oracle.truffle.lama.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

public final class LamaSequenceNode extends LamaExpressionNode {
    @Children private final LamaExpressionNode[] expressions;

    public LamaSequenceNode(LamaExpressionNode[] expressions) {
        this.expressions = expressions;
    }

    public LamaExpressionNode[] getExpressions() {
        return expressions;
    }

    @Override
    @ExplodeLoop
    public Object executeGeneric(VirtualFrame frame) {
        Object value = 0L;
        for (LamaExpressionNode expression : expressions) {
            value = expression.executeGeneric(frame);
        }
        return value;
    }
}
