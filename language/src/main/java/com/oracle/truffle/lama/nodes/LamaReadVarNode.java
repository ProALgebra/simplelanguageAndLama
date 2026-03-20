package com.oracle.truffle.lama.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.lama.LamaContext;
import com.oracle.truffle.lama.runtime.LamaCell;
import com.oracle.truffle.lama.runtime.LamaFunctionTemplate;

public final class LamaReadVarNode extends LamaExpressionNode {
    private final String name;

    public LamaReadVarNode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        LamaCell cell = LamaContext.get(this).lookupCell(name);
        Object value = cell.get();
        if (value == null) {
            return 0L;
        }
        if (value instanceof LamaFunctionTemplate template) {
            return template.instantiate();
        }
        return value;
    }
}
