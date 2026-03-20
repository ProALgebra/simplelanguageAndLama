package com.oracle.truffle.lama.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.lama.LamaContext;

public final class LamaScopeNode extends LamaExpressionNode {
    @Children private final LamaExpressionNode[] definitions;
    @Child private LamaExpressionNode body;

    public LamaScopeNode(LamaExpressionNode[] definitions, LamaExpressionNode body) {
        this.definitions = definitions;
        this.body = body;
    }

    public LamaExpressionNode[] getDefinitions() {
        return definitions;
    }

    public LamaExpressionNode getBody() {
        return body;
    }

    @Override
    @ExplodeLoop
    public Object executeGeneric(VirtualFrame frame) {
        LamaContext context = LamaContext.get(this);
        context.pushEnv();
        try {
            for (LamaExpressionNode definition : definitions) {
                definition.executeGeneric(frame);
            }
            return body.executeGeneric(frame);
        } finally {
            context.popEnv();
        }
    }
}
