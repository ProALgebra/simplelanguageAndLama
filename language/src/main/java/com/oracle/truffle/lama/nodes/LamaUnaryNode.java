package com.oracle.truffle.lama.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;

public final class LamaUnaryNode extends LamaExpressionNode {
    public enum Operator {
        PLUS,
        MINUS,
    }

    private final Operator operator;
    @Child private LamaExpressionNode operand;

    public LamaUnaryNode(Operator operator, LamaExpressionNode operand) {
        this.operator = operator;
        this.operand = operand;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        long value = operand.executeLong(frame);
        return switch (operator) {
            case PLUS -> value;
            case MINUS -> -value;
        };
    }
}

