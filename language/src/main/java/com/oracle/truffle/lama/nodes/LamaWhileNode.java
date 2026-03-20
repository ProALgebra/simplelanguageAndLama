package com.oracle.truffle.lama.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;

public final class LamaWhileNode extends LamaExpressionNode {
    @Child private LamaExpressionNode condition;
    @Child private LamaExpressionNode body;

    public LamaWhileNode(LamaExpressionNode condition, LamaExpressionNode body) {
        this.condition = condition;
        this.body = body;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        while (condition.executeLong(frame) != 0) {
            body.executeGeneric(frame);
        }
        return 0L;
    }
}

