package com.oracle.truffle.lama.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.lama.runtime.LamaSexp;

public final class LamaSexpLiteralNode extends LamaExpressionNode {
    private final String tag;
    @Children private final LamaExpressionNode[] fields;

    public LamaSexpLiteralNode(String tag, LamaExpressionNode[] fields) {
        this.tag = tag;
        this.fields = fields;
    }

    @Override
    @ExplodeLoop
    public Object executeGeneric(VirtualFrame frame) {
        Object[] values = new Object[fields.length];
        for (int i = 0; i < fields.length; i++) {
            values[i] = fields[i].executeGeneric(frame);
        }
        return new LamaSexp(tag, values);
    }
}

