package com.oracle.truffle.lama.runtime;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

@ExportLibrary(InteropLibrary.class)
public final class LamaSexp implements TruffleObject {
    private final String tag;
    private final Object[] fields;

    public LamaSexp(String tag, Object[] fields) {
        this.tag = tag;
        this.fields = fields;
    }

    public String tag() {
        return tag;
    }

    public int length() {
        return fields.length;
    }

    public Object get(int index) {
        Object v = fields[index];
        return v == null ? 0L : v;
    }

    public void set(int index, Object value) {
        fields[index] = value;
    }

    @ExportMessage
    boolean hasArrayElements() {
        return true;
    }

    @ExportMessage
    long getArraySize() {
        return fields.length;
    }

    @ExportMessage
    boolean isArrayElementReadable(long index) {
        return index >= 0 && index < fields.length;
    }

    @ExportMessage
    Object readArrayElement(long index) throws InvalidArrayIndexException {
        if (!isArrayElementReadable(index)) {
            throw InvalidArrayIndexException.create(index);
        }
        return get((int) index);
    }

    @ExportMessage
    boolean hasMembers() {
        return true;
    }

    @ExportMessage
    Object getMembers(boolean includeInternal) throws UnsupportedMessageException {
        if (!hasMembers()) {
            throw UnsupportedMessageException.create();
        }
        return new String[]{"tag", "length"};
    }

    @ExportMessage
    boolean isMemberReadable(String member) {
        return "tag".equals(member) || "length".equals(member);
    }

    @ExportMessage
    Object readMember(String member) throws UnknownIdentifierException {
        if ("tag".equals(member)) {
            return tag;
        }
        if ("length".equals(member)) {
            return (long) fields.length;
        }
        throw UnknownIdentifierException.create(member);
    }
}
