package com.oracle.truffle.lama.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import java.nio.charset.StandardCharsets;

public final class LamaStringLiteralNode extends LamaExpressionNode {
    private final byte[] bytes;

    public LamaStringLiteralNode(String value) {
        this.bytes = value.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return bytes.clone();
    }
}

