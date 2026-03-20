package com.oracle.truffle.lama.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.lama.LamaContext;
import com.oracle.truffle.lama.runtime.LamaCell;
import com.oracle.truffle.lama.runtime.LamaLValue;

public final class LamaVarLValueNode extends LamaExpressionNode {
    private final String name;

    public LamaVarLValueNode(String name) {
        this.name = name;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        LamaContext context = LamaContext.get(this);
        return new LamaLValue() {
            @Override
            public Object get() {
                LamaCell cell = context.lookupCell(name);
                Object value = cell.get();
                return value == null ? 0L : value;
            }

            @Override
            public void set(Object value) {
                context.lookupCell(name).set(value);
            }
        };
    }
}
