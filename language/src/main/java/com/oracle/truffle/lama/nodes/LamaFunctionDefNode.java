package com.oracle.truffle.lama.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.lama.LamaContext;
import com.oracle.truffle.lama.runtime.LamaCell;
import com.oracle.truffle.lama.runtime.LamaFunctionTemplate;
import com.oracle.truffle.lama.runtime.pattern.LamaPattern;
import java.util.List;

public final class LamaFunctionDefNode extends LamaExpressionNode {
    private final String name;
    private final List<LamaPattern> parameters;
    @Child private LamaExpressionNode body;

    public LamaFunctionDefNode(String name, List<LamaPattern> parameters, LamaExpressionNode body) {
        this.name = name;
        this.parameters = parameters;
        this.body = body;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        LamaContext context = LamaContext.get(this);
        LamaCell cell = context.declareImmutable(name);
        cell.init(new LamaFunctionTemplate(parameters, body, context.getCurrentEnv()));
        return 0L;
    }
}
