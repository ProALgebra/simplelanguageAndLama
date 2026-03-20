package com.oracle.truffle.lama.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;

public final class LamaBinaryNode extends LamaExpressionNode {
    public enum Operator {
        ADD,
        SUB,
        MUL,
        DIV,
        MOD,
        LT,
        LE,
        GT,
        GE,
        EQ,
        NE,
        AND,
        OR,
    }

    private final Operator operator;
    @Child private LamaExpressionNode left;
    @Child private LamaExpressionNode right;

    public LamaBinaryNode(Operator operator, LamaExpressionNode left, LamaExpressionNode right) {
        this.operator = operator;
        this.left = left;
        this.right = right;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        long lhs = left.executeLong(frame);
        long rhs = right.executeLong(frame);
        return switch (operator) {
            case ADD -> lhs + rhs;
            case SUB -> lhs - rhs;
            case MUL -> lhs * rhs;
            case DIV -> {
                if (rhs == 0) {
                    throw new ArithmeticException("division by zero");
                }
                yield lhs / rhs;
            }
            case MOD -> {
                if (rhs == 0) {
                    throw new ArithmeticException("division by zero");
                }
                yield lhs % rhs;
            }
            case LT -> lhs < rhs ? 1L : 0L;
            case LE -> lhs <= rhs ? 1L : 0L;
            case GT -> lhs > rhs ? 1L : 0L;
            case GE -> lhs >= rhs ? 1L : 0L;
            case EQ -> lhs == rhs ? 1L : 0L;
            case NE -> lhs != rhs ? 1L : 0L;
            case AND -> (lhs != 0 && rhs != 0) ? 1L : 0L;
            case OR -> (lhs != 0 || rhs != 0) ? 1L : 0L;
        };
    }
}
