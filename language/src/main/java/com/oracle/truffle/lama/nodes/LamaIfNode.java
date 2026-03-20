package com.oracle.truffle.lama.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;

public final class LamaIfNode extends LamaExpressionNode {
    @Child private LamaExpressionNode condition;
    @Child private LamaExpressionNode thenBranch;
    @Child private LamaExpressionNode elseBranch;

    public LamaIfNode(LamaExpressionNode condition, LamaExpressionNode thenBranch, LamaExpressionNode elseBranch) {
        this.condition = condition;
        this.thenBranch = thenBranch;
        this.elseBranch = elseBranch;
    }

    public LamaExpressionNode getCondition() {
        return condition;
    }

    public LamaExpressionNode getThenBranch() {
        return thenBranch;
    }

    public LamaExpressionNode getElseBranch() {
        return elseBranch;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        long cond = condition.executeLong(frame);
        if (cond != 0) {
            return thenBranch.executeGeneric(frame);
        }
        return elseBranch.executeGeneric(frame);
    }
}
