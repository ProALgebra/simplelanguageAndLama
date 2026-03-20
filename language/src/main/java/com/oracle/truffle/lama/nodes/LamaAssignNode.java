package com.oracle.truffle.lama.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.lama.runtime.LamaLValue;

public final class LamaAssignNode extends LamaExpressionNode {
    @Child private LamaExpressionNode target;
    @Child private LamaExpressionNode value;

    public LamaAssignNode(LamaExpressionNode target, LamaExpressionNode value) {
        this.target = target;
        this.value = value;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        Object t = target.executeGeneric(frame);
        if (!(t instanceof LamaLValue)) {
            throw new RuntimeException("Left side of ':=' is not assignable");
        }
        Object v = value.executeGeneric(frame);
        ((LamaLValue) t).set(v);
        return v;
    }
}

