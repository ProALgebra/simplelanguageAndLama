package com.oracle.truffle.lama.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;

public final class LamaDoWhileNode extends LamaExpressionNode {
    @Child private LamaExpressionNode body;
    @Child private LamaExpressionNode condition;

    public LamaDoWhileNode(LamaExpressionNode body, LamaExpressionNode condition) {
        this.body = body;
        this.condition = condition;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        do {
            body.executeGeneric(frame);
        } while (condition.executeLong(frame) != 0);
        return 0L;
    }
}

