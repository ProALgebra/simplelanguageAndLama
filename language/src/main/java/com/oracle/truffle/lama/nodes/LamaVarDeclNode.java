package com.oracle.truffle.lama.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.lama.LamaContext;
import java.util.List;

public final class LamaVarDeclNode extends LamaExpressionNode {
    public record Decl(String name, LamaExpressionNode initializer) {
    }

    private final List<Decl> decls;

    public LamaVarDeclNode(List<Decl> decls) {
        this.decls = decls;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        LamaContext context = LamaContext.get(this);
        for (Decl decl : decls) {
            Object value = decl.initializer == null ? 0L : decl.initializer.executeGeneric(frame);
            context.declareVar(decl.name, value);
        }
        return 0L;
    }
}

