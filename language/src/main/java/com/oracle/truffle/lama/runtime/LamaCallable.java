package com.oracle.truffle.lama.runtime;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.lama.LamaContext;

public interface LamaCallable {
    Object call(LamaContext context, VirtualFrame frame, Object[] args);
}

