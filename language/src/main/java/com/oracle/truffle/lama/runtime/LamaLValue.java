package com.oracle.truffle.lama.runtime;

public interface LamaLValue {
    Object get();

    void set(Object value);
}

