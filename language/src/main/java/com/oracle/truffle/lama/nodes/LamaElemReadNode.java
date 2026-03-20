package com.oracle.truffle.lama.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.lama.runtime.LamaSexp;

public final class LamaElemReadNode extends LamaExpressionNode {
    @Child private LamaExpressionNode receiver;
    @Child private LamaExpressionNode index;

    public LamaElemReadNode(LamaExpressionNode receiver, LamaExpressionNode index) {
        this.receiver = receiver;
        this.index = index;
    }

    public LamaExpressionNode getReceiver() {
        return receiver;
    }

    public LamaExpressionNode getIndex() {
        return index;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        Object r = receiver.executeGeneric(frame);
        int i = (int) index.executeLong(frame);

        if (r instanceof byte[] bytes) {
            return (long) (bytes[i] & 0xFF);
        }
        if (r instanceof Object[] arr) {
            Object v = arr[i];
            return v == null ? 0L : v;
        }
        if (r instanceof LamaSexp sexp) {
            return sexp.get(i);
        }
        throw new RuntimeException("Unsupported element access");
    }
}
