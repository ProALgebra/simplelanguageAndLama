package com.oracle.truffle.lama.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;

public final class LamaForNode extends LamaExpressionNode {
    @Child private LamaExpressionNode init;
    @Child private LamaExpressionNode condition;
    @Child private LamaExpressionNode step;
    @Child private LamaExpressionNode body;

    public LamaForNode(LamaExpressionNode init, LamaExpressionNode condition, LamaExpressionNode step, LamaExpressionNode body) {
        this.init = init;
        this.condition = condition;
        this.step = step;
        this.body = body;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        init.executeGeneric(frame);
        while (condition.executeLong(frame) != 0) {
            body.executeGeneric(frame);
            step.executeGeneric(frame);
        }
        return 0L;
    }
}

