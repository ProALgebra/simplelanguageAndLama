package com.oracle.truffle.lama.runtime;

public final class LamaCell {
    private final boolean mutable;
    private Object value;

    public LamaCell(boolean mutable, Object value) {
        this.mutable = mutable;
        this.value = value;
    }

    public boolean isMutable() {
        return mutable;
    }

    public Object get() {
        return value;
    }

    public void set(Object value) {
        if (!mutable) {
            throw new RuntimeException("Attempt to assign to immutable binding");
        }
        this.value = value;
    }

    public void init(Object value) {
        if (this.value != null) {
            throw new RuntimeException("Attempt to initialize an already initialized binding");
        }
        this.value = value;
    }
}
