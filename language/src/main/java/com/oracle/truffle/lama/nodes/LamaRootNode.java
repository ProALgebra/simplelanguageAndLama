package com.oracle.truffle.lama.nodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.lama.LamaLanguage;

public final class LamaRootNode extends RootNode {
    @Child private LamaExpressionNode body;
    private final SourceSection sourceSection;

    public LamaRootNode(LamaLanguage language, FrameDescriptor frameDescriptor, LamaExpressionNode body, SourceSection sourceSection) {
        super(language, frameDescriptor);
        this.body = body;
        this.sourceSection = sourceSection;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return body.executeGeneric(frame);
    }

    @Override
    public SourceSection getSourceSection() {
        return sourceSection;
    }
}

