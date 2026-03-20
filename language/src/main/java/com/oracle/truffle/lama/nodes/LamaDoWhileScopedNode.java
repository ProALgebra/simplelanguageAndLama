package com.oracle.truffle.lama.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.lama.LamaContext;

public final class LamaDoWhileScopedNode extends LamaExpressionNode {
    @Child private LamaExpressionNode body;
    @Child private LamaExpressionNode condition;

    public LamaDoWhileScopedNode(LamaExpressionNode body, LamaExpressionNode condition) {
        this.body = body;
        this.condition = condition;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        LamaContext context = LamaContext.get(this);
        while (true) {
            long cond;
            context.pushEnv();
            try {
                body.executeGeneric(frame);
                cond = condition.executeLong(frame);
            } finally {
                context.popEnv();
            }
            if (cond == 0) {
                return 0L;
            }
        }
    }
}

