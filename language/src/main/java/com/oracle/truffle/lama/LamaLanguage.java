package com.oracle.truffle.lama;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.TruffleLanguage.ContextPolicy;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.lama.nodes.LamaExpressionNode;
import com.oracle.truffle.lama.nodes.LamaRootNode;
import com.oracle.truffle.lama.parser.LamaNodeParser;

@TruffleLanguage.Registration(id = LamaLanguage.ID, name = "Lama", defaultMimeType = LamaLanguage.MIME_TYPE, characterMimeTypes = LamaLanguage.MIME_TYPE, contextPolicy = ContextPolicy.SHARED, fileTypeDetectors = LamaFileDetector.class)
public final class LamaLanguage extends TruffleLanguage<LamaContext> {
    public static final String ID = "lama";
    public static final String MIME_TYPE = "application/x-lama";

    @Override
    protected LamaContext createContext(Env env) {
        return new LamaContext(env);
    }

    @Override
    protected CallTarget parse(ParsingRequest request) throws Exception {
        Source source = request.getSource();
        LamaExpressionNode body = LamaNodeParser.parse(source);
        RootNode root = new LamaRootNode(this, FrameDescriptor.newBuilder().build(), body, source.createUnavailableSection());
        return root.getCallTarget();
    }
}
