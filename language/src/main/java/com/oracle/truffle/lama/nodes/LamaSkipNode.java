package com.oracle.truffle.lama.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;

public final class LamaSkipNode extends LamaExpressionNode {
    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return 0L;
    }
}

