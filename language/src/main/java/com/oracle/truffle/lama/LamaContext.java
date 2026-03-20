package com.oracle.truffle.lama;

import com.oracle.truffle.api.TruffleLanguage.ContextReference;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.lama.runtime.LamaCell;
import com.oracle.truffle.lama.runtime.LamaEnv;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.StringTokenizer;

public final class LamaContext {
    private static final ContextReference<LamaContext> REFERENCE = ContextReference.create(LamaLanguage.class);

    private final TruffleLanguage.Env env;
    private final PrintWriter out;
    private final BufferedReader in;

    private StringTokenizer tokenizer;

    private final LamaEnv globalEnv;
    private LamaEnv currentEnv;
    private final Deque<LamaEnv> envStack = new ArrayDeque<>();

    LamaContext(TruffleLanguage.Env env) {
        this.env = env;
        this.out = new PrintWriter(env.out(), true);
        this.in = new BufferedReader(new InputStreamReader(env.in()));
        this.globalEnv = new LamaEnv(null);
        this.currentEnv = globalEnv;
        envStack.push(globalEnv);
    }

    public static LamaContext get(Node node) {
        return REFERENCE.get(node);
    }

    public TruffleLanguage.Env getEnv() {
        return env;
    }

    public LamaEnv getCurrentEnv() {
        return currentEnv;
    }

    public LamaEnv getGlobalEnv() {
        return globalEnv;
    }

    public void pushEnv() {
        pushEnvWithParent(currentEnv);
    }

    public void pushEnvWithParent(LamaEnv parent) {
        currentEnv = new LamaEnv(parent);
        envStack.push(currentEnv);
    }

    public void popEnv() {
        if (envStack.size() <= 1) {
            throw new IllegalStateException("Attempt to pop global env");
        }
        envStack.pop();
        currentEnv = envStack.peek();
    }

    public LamaCell declareVar(String name, Object initialValue) {
        return currentEnv.declare(name, true, initialValue);
    }

    public LamaCell declareImmutable(String name, Object value) {
        return currentEnv.declare(name, false, value);
    }

    public LamaCell declareImmutable(String name) {
        return currentEnv.declare(name, false, null);
    }

    public LamaCell lookupCell(String name) {
        return currentEnv.lookupCell(name);
    }

    public long readInt() {
        out.print("> ");
        out.flush();
        while (tokenizer == null || !tokenizer.hasMoreTokens()) {
            String line;
            try {
                line = in.readLine();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (line == null) {
                throw new RuntimeException("Unexpected end of input");
            }
            tokenizer = new StringTokenizer(line);
        }
        return Long.parseLong(tokenizer.nextToken());
    }

    public void writeLong(long value) {
        out.println(value);
        out.flush();
    }
}
