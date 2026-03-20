package com.oracle.truffle.lama.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;

public final class LamaReadIdentifierNode extends LamaExpressionNode {
    private final String name;

    public LamaReadIdentifierNode(String name) {
        this.name = name;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        CompilerDirectives.transferToInterpreter();
        throw new RuntimeException("Unknown identifier: " + name);
    }
}

