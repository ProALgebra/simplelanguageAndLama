package com.oracle.truffle.lama.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.lama.LamaContext;
import com.oracle.truffle.lama.runtime.LamaCallable;

public final class LamaIndirectCallNode extends LamaExpressionNode {
    @Child private LamaExpressionNode calleeExpr;
    @Children private final LamaExpressionNode[] arguments;

    public LamaIndirectCallNode(LamaExpressionNode calleeExpr, LamaExpressionNode[] arguments) {
        this.calleeExpr = calleeExpr;
        this.arguments = arguments;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        Object target = calleeExpr.executeGeneric(frame);
        if (!(target instanceof LamaCallable callable)) {
            CompilerDirectives.transferToInterpreter();
            throw new RuntimeException("Callee is not callable");
        }
        Object[] args = new Object[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            args[i] = arguments[i].executeGeneric(frame);
        }
        return callable.call(LamaContext.get(this), frame, args);
    }
}
