package com.oracle.truffle.lama.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.lama.runtime.LamaLValue;
import com.oracle.truffle.lama.runtime.LamaSexp;

public final class LamaElemLValueNode extends LamaExpressionNode {
    @Child private LamaExpressionNode receiver;
    @Child private LamaExpressionNode index;

    public LamaElemLValueNode(LamaExpressionNode receiver, LamaExpressionNode index) {
        this.receiver = receiver;
        this.index = index;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        Object r = receiver.executeGeneric(frame);
        int i = (int) index.executeLong(frame);

        if (r instanceof byte[] bytes) {
            return new LamaLValue() {
                @Override
                public Object get() {
                    return (long) (bytes[i] & 0xFF);
                }

                @Override
                public void set(Object value) {
                    bytes[i] = (byte) ((Number) value).longValue();
                }
            };
        }

        if (r instanceof Object[] arr) {
            return new LamaLValue() {
                @Override
                public Object get() {
                    Object v = arr[i];
                    return v == null ? 0L : v;
                }

                @Override
                public void set(Object value) {
                    arr[i] = value;
                }
            };
        }

        if (r instanceof LamaSexp sexp) {
            return new LamaLValue() {
                @Override
                public Object get() {
                    return sexp.get(i);
                }

                @Override
                public void set(Object value) {
                    sexp.set(i, value);
                }
            };
        }

        throw new RuntimeException("Unsupported element assignment");
    }
}
