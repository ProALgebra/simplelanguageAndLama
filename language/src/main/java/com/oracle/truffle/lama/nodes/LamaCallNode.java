package com.oracle.truffle.lama.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.lama.LamaContext;
import com.oracle.truffle.lama.runtime.LamaCallable;
import com.oracle.truffle.lama.runtime.LamaSexp;
import java.nio.charset.StandardCharsets;

public final class LamaCallNode extends LamaExpressionNode {
    private final String callee;
    @Children private final LamaExpressionNode[] arguments;

    public LamaCallNode(String callee, LamaExpressionNode[] arguments) {
        this.callee = callee;
        this.arguments = arguments;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        LamaContext context = LamaContext.get(this);

        if ("read".equals(callee)) {
            if (arguments.length != 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RuntimeException("read() expects 0 arguments");
            }
            return context.readInt();
        }

        if ("write".equals(callee)) {
            if (arguments.length != 1) {
                CompilerDirectives.transferToInterpreter();
                throw new RuntimeException("write(x) expects 1 argument");
            }
            long value = arguments[0].executeLong(frame);
            context.writeLong(value);
            return 0L;
        }

        if ("length".equals(callee)) {
            if (arguments.length != 1) {
                CompilerDirectives.transferToInterpreter();
                throw new RuntimeException("length(x) expects 1 argument");
            }
            Object v = arguments[0].executeGeneric(frame);
            if (v instanceof byte[] bytes) {
                return (long) bytes.length;
            }
            if (v instanceof Object[] arr) {
                return (long) arr.length;
            }
            if (v instanceof LamaSexp sexp) {
                return (long) sexp.length();
            }
            CompilerDirectives.transferToInterpreter();
            throw new RuntimeException("length(x) unsupported type");
        }

        if ("string".equals(callee)) {
            if (arguments.length != 1) {
                CompilerDirectives.transferToInterpreter();
                throw new RuntimeException("string(x) expects 1 argument");
            }
            Object v = arguments[0].executeGeneric(frame);
            StringBuilder sb = new StringBuilder();
            appendStringValue(sb, v);
            return sb.toString().getBytes(StandardCharsets.UTF_8);
        }

        Object[] args = new Object[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            args[i] = arguments[i].executeGeneric(frame);
        }
        Object target = context.lookupCell(callee).get();
        if (target instanceof LamaCallable callable) {
            return callable.call(context, frame, args);
        }
        CompilerDirectives.transferToInterpreter();
        throw new RuntimeException("Callee is not callable: " + callee);
    }

    private static void appendStringValue(StringBuilder sb, Object value) {
        Object v = value == null ? 0L : value;
        if (v instanceof Number n) {
            sb.append(n.longValue());
            return;
        }
        if (v instanceof byte[] bytes) {
            sb.append('"');
            sb.append(new String(bytes, StandardCharsets.UTF_8));
            sb.append('"');
            return;
        }
        if (v instanceof Object[] arr) {
            sb.append('[');
            for (int i = 0; i < arr.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                appendStringValue(sb, arr[i]);
            }
            sb.append(']');
            return;
        }
        if (v instanceof LamaSexp sexp) {
            sb.append(sexp.tag());
            if (sexp.length() > 0) {
                sb.append(" (");
                for (int i = 0; i < sexp.length(); i++) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    appendStringValue(sb, sexp.get(i));
                }
                sb.append(')');
            }
            return;
        }
        sb.append(v);
    }
}
