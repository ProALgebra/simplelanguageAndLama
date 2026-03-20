package com.oracle.truffle.lama.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

public final class LamaArrayLiteralNode extends LamaExpressionNode {
    @Children private final LamaExpressionNode[] elements;

    public LamaArrayLiteralNode(LamaExpressionNode[] elements) {
        this.elements = elements;
    }

    @Override
    @ExplodeLoop
    public Object executeGeneric(VirtualFrame frame) {
        Object[] result = new Object[elements.length];
        for (int i = 0; i < elements.length; i++) {
            result[i] = elements[i].executeGeneric(frame);
        }
        return result;
    }
}

