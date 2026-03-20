package com.oracle.truffle.lama.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

public abstract class LamaExpressionNode extends Node {
    public abstract Object executeGeneric(VirtualFrame frame);

    public long executeLong(VirtualFrame frame) {
        return (long) executeGeneric(frame);
    }
}

