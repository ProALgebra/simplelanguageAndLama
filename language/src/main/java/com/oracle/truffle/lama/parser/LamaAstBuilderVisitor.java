package com.oracle.truffle.lama.parser;

import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.lama.nodes.LamaArrayLiteralNode;
import com.oracle.truffle.lama.nodes.LamaAssignNode;
import com.oracle.truffle.lama.nodes.LamaBinaryNode;
import com.oracle.truffle.lama.nodes.LamaCallNode;
import com.oracle.truffle.lama.nodes.LamaCaseNode;
import com.oracle.truffle.lama.nodes.LamaDoWhileScopedNode;
import com.oracle.truffle.lama.nodes.LamaElemLValueNode;
import com.oracle.truffle.lama.nodes.LamaElemReadNode;
import com.oracle.truffle.lama.nodes.LamaExpressionNode;
import com.oracle.truffle.lama.nodes.LamaForNode;
import com.oracle.truffle.lama.nodes.LamaFunctionDefNode;
import com.oracle.truffle.lama.nodes.LamaIfNode;
import com.oracle.truffle.lama.nodes.LamaIndirectCallNode;
import com.oracle.truffle.lama.nodes.LamaLambdaNode;
import com.oracle.truffle.lama.nodes.LamaLongLiteralNode;
import com.oracle.truffle.lama.nodes.LamaReadVarNode;
import com.oracle.truffle.lama.nodes.LamaScopeNode;
import com.oracle.truffle.lama.nodes.LamaSequenceNode;
import com.oracle.truffle.lama.nodes.LamaSexpLiteralNode;
import com.oracle.truffle.lama.nodes.LamaSkipNode;
import com.oracle.truffle.lama.nodes.LamaStringLiteralNode;
import com.oracle.truffle.lama.nodes.LamaUnaryNode;
import com.oracle.truffle.lama.nodes.LamaVarDeclNode;
import com.oracle.truffle.lama.nodes.LamaVarLValueNode;
import com.oracle.truffle.lama.nodes.LamaWhileNode;
import com.oracle.truffle.lama.runtime.pattern.LamaArrayPattern;
import com.oracle.truffle.lama.runtime.pattern.LamaAsPattern;
import com.oracle.truffle.lama.runtime.pattern.LamaBindPattern;
import com.oracle.truffle.lama.runtime.pattern.LamaLongPattern;
import com.oracle.truffle.lama.runtime.pattern.LamaPattern;
import com.oracle.truffle.lama.runtime.pattern.LamaSexpPattern;
import com.oracle.truffle.lama.runtime.pattern.LamaStringPattern;
import com.oracle.truffle.lama.runtime.pattern.LamaTypePattern;
import com.oracle.truffle.lama.runtime.pattern.LamaWildcardPattern;
import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

final class LamaAstBuilderVisitor extends LamaLanguageBaseVisitor<LamaExpressionNode> {
    private final Source source;

    LamaAstBuilderVisitor(Source source) {
        this.source = source;
    }

    @Override
    public LamaExpressionNode visitProgram(LamaLanguageParser.ProgramContext ctx) {
        List<LamaExpressionNode> items = new ArrayList<>();
        for (var item : ctx.topLevelItem()) {
            items.add(visit(item));
        }
        return asSequence(items);
    }

    @Override
    public LamaExpressionNode visitTopLevelItem(LamaLanguageParser.TopLevelItemContext ctx) {
        if (ctx.definition() != null) {
            return visit(ctx.definition());
        }
        return visit(ctx.expression());
    }

    @Override
    public LamaExpressionNode visitDefinition(LamaLanguageParser.DefinitionContext ctx) {
        if (ctx.varDefinition() != null) {
            return visit(ctx.varDefinition());
        }
        if (ctx.functionDefinition() != null) {
            return visit(ctx.functionDefinition());
        }
        return visit(ctx.infixDefinition());
    }

    @Override
    public LamaExpressionNode visitVarDefinition(LamaLanguageParser.VarDefinitionContext ctx) {
        List<LamaVarDeclNode.Decl> decls = new ArrayList<>();
        for (var binding : ctx.varBinding()) {
            LamaExpressionNode init = null;
            if (binding.assignmentExpression() != null) {
                init = visit(binding.assignmentExpression());
            }
            decls.add(new LamaVarDeclNode.Decl(binding.IDENTIFIER().getText(), init));
        }
        return new LamaVarDeclNode(decls);
    }

    @Override
    public LamaExpressionNode visitFunctionDefinition(LamaLanguageParser.FunctionDefinitionContext ctx) {
        List<LamaPattern> params = bindParamsFromParameterList(ctx.parameterList());
        LamaExpressionNode body = visit(ctx.blockExpression());
        return new LamaFunctionDefNode(ctx.IDENTIFIER().getText(), params, body);
    }

    @Override
    public LamaExpressionNode visitInfixDefinition(LamaLanguageParser.InfixDefinitionContext ctx) {
        String name = ctx.operatorSymbol(0).getText();
        List<LamaPattern> params = bindParamsFromParameterList(ctx.parameterList());
        LamaExpressionNode body = visit(ctx.blockExpression());
        return new LamaFunctionDefNode(name, params, body);
    }

    @Override
    public LamaExpressionNode visitBlockExpression(LamaLanguageParser.BlockExpressionContext ctx) {
        return scopedBodyNode(ctx.scopedBody(), ctx);
    }

    @Override
    public LamaExpressionNode visitExpression(LamaLanguageParser.ExpressionContext ctx) {
        return visit(ctx.sequenceExpression());
    }

    @Override
    public LamaExpressionNode visitSequenceExpression(LamaLanguageParser.SequenceExpressionContext ctx) {
        List<LamaExpressionNode> seq = new ArrayList<>();
        for (var assignment : ctx.assignmentExpression()) {
            seq.add(visit(assignment));
        }
        return asSequence(seq);
    }

    @Override
    public LamaExpressionNode visitAssignmentExpression(LamaLanguageParser.AssignmentExpressionContext ctx) {
        LamaExpressionNode left = visit(ctx.consExpression());
        if (ctx.assignmentExpression() == null) {
            return left;
        }
        LamaExpressionNode target = asLValue(left, ctx);
        LamaExpressionNode value = visit(ctx.assignmentExpression());
        return new LamaAssignNode(target, value);
    }

    @Override
    public LamaExpressionNode visitConsExpression(LamaLanguageParser.ConsExpressionContext ctx) {
        LamaExpressionNode head = visit(ctx.orExpression());
        if (ctx.consExpression() == null) {
            return head;
        }
        LamaExpressionNode tail = visit(ctx.consExpression());
        return new LamaSexpLiteralNode("Cons", new LamaExpressionNode[]{head, tail});
    }

    @Override
    public LamaExpressionNode visitOrExpression(LamaLanguageParser.OrExpressionContext ctx) {
        LamaExpressionNode node = visit(ctx.andExpression(0));
        for (int i = 1; i < ctx.andExpression().size(); i++) {
            String op = ctx.getChild(2 * i - 1).getText();
            LamaBinaryNode.Operator kind = "!!".equals(op) || "||".equals(op)
                            ? LamaBinaryNode.Operator.OR
                            : null;
            if (kind == null) {
                throw fail(ctx, "unsupported logical operator: " + op);
            }
            node = new LamaBinaryNode(kind, node, visit(ctx.andExpression(i)));
        }
        return node;
    }

    @Override
    public LamaExpressionNode visitAndExpression(LamaLanguageParser.AndExpressionContext ctx) {
        LamaExpressionNode node = visit(ctx.customInfixExpression(0));
        for (int i = 1; i < ctx.customInfixExpression().size(); i++) {
            node = new LamaBinaryNode(LamaBinaryNode.Operator.AND, node, visit(ctx.customInfixExpression(i)));
        }
        return node;
    }

    @Override
    public LamaExpressionNode visitCustomInfixExpression(LamaLanguageParser.CustomInfixExpressionContext ctx) {
        LamaExpressionNode node = visit(ctx.compareExpression(0));
        for (int i = 1; i < ctx.compareExpression().size(); i++) {
            String op = ctx.getChild(2 * i - 1).getText();
            node = buildBinary(op, node, visit(ctx.compareExpression(i)), ctx);
        }
        return node;
    }

    @Override
    public LamaExpressionNode visitCompareExpression(LamaLanguageParser.CompareExpressionContext ctx) {
        LamaExpressionNode left = visit(ctx.additiveExpression(0));
        if (ctx.additiveExpression().size() == 1) {
            return left;
        }
        LamaExpressionNode right = visit(ctx.additiveExpression(1));
        String op = ctx.getChild(1).getText();
        return new LamaBinaryNode(switch (op) {
            case "<" -> LamaBinaryNode.Operator.LT;
            case "<=" -> LamaBinaryNode.Operator.LE;
            case ">" -> LamaBinaryNode.Operator.GT;
            case ">=" -> LamaBinaryNode.Operator.GE;
            case "=" -> LamaBinaryNode.Operator.EQ;
            case "==" -> LamaBinaryNode.Operator.EQ;
            case "!=" -> LamaBinaryNode.Operator.NE;
            default -> throw fail(ctx, "unsupported comparison operator: " + op);
        }, left, right);
    }

    @Override
    public LamaExpressionNode visitAdditiveExpression(LamaLanguageParser.AdditiveExpressionContext ctx) {
        LamaExpressionNode node = visit(ctx.multiplicativeExpression(0));
        for (int i = 1; i < ctx.multiplicativeExpression().size(); i++) {
            String op = ctx.getChild(2 * i - 1).getText();
            LamaBinaryNode.Operator kind = "+".equals(op) ? LamaBinaryNode.Operator.ADD : LamaBinaryNode.Operator.SUB;
            node = new LamaBinaryNode(kind, node, visit(ctx.multiplicativeExpression(i)));
        }
        return node;
    }

    @Override
    public LamaExpressionNode visitMultiplicativeExpression(LamaLanguageParser.MultiplicativeExpressionContext ctx) {
        LamaExpressionNode node = visit(ctx.unaryExpression(0));
        for (int i = 1; i < ctx.unaryExpression().size(); i++) {
            String op = ctx.getChild(2 * i - 1).getText();
            LamaBinaryNode.Operator kind = switch (op) {
                case "*" -> LamaBinaryNode.Operator.MUL;
                case "/" -> LamaBinaryNode.Operator.DIV;
                case "%" -> LamaBinaryNode.Operator.MOD;
                default -> throw fail(ctx, "unsupported multiplicative operator: " + op);
            };
            node = new LamaBinaryNode(kind, node, visit(ctx.unaryExpression(i)));
        }
        return node;
    }

    @Override
    public LamaExpressionNode visitUnaryExpression(LamaLanguageParser.UnaryExpressionContext ctx) {
        if (ctx.postfixExpression() != null) {
            return visit(ctx.postfixExpression());
        }
        LamaExpressionNode operand = visit(ctx.unaryExpression());
        if (ctx.ETA() != null) {
            return operand;
        }
        if (ctx.MINUS() != null) {
            return new LamaUnaryNode(LamaUnaryNode.Operator.MINUS, operand);
        }
        return new LamaUnaryNode(LamaUnaryNode.Operator.PLUS, operand);
    }

    @Override
    public LamaExpressionNode visitPostfixExpression(LamaLanguageParser.PostfixExpressionContext ctx) {
        boolean hadPostfix = !ctx.postfixPart().isEmpty();
        LamaExpressionNode current = visit(ctx.primaryExpression());
        for (var part : ctx.postfixPart()) {
            if (part.LPAREN() != null) {
                LamaExpressionNode[] args = visitArgs(part.argumentList());
                if (current instanceof LamaReadVarNode named) {
                    String name = named.getName();
                    if (isConstructorName(name)) {
                        current = new LamaSexpLiteralNode(name, args);
                    } else {
                        current = new LamaCallNode(name, args);
                    }
                } else {
                    current = new LamaIndirectCallNode(current, args);
                }
            } else if (part.DOT() != null) {
                String member = part.IDENTIFIER().getText();
                current = new LamaCallNode(member, new LamaExpressionNode[]{current});
            } else {
                current = new LamaElemReadNode(current, visit(part.expression()));
            }
        }
        if (!hadPostfix && current instanceof LamaReadVarNode named && isConstructorName(named.getName())) {
            return new LamaSexpLiteralNode(named.getName(), new LamaExpressionNode[0]);
        }
        return current;
    }

    @Override
    public LamaExpressionNode visitPrimaryExpression(LamaLanguageParser.PrimaryExpressionContext ctx) {
        if (ctx.literal() != null) {
            return visit(ctx.literal());
        }
        if (ctx.IDENTIFIER() != null) {
            return new LamaReadVarNode(ctx.IDENTIFIER().getText());
        }
        if (ctx.ifExpression() != null) {
            return visit(ctx.ifExpression());
        }
        if (ctx.whileExpression() != null) {
            return visit(ctx.whileExpression());
        }
        if (ctx.doWhileExpression() != null) {
            return visit(ctx.doWhileExpression());
        }
        if (ctx.forExpression() != null) {
            return visit(ctx.forExpression());
        }
        if (ctx.caseExpression() != null) {
            return visit(ctx.caseExpression());
        }
        if (ctx.letExpression() != null) {
            return visit(ctx.letExpression());
        }
        if (ctx.funLiteral() != null) {
            return visit(ctx.funLiteral());
        }
        if (ctx.infixReference() != null) {
            return visit(ctx.infixReference());
        }
        if (ctx.KW_SKIP() != null) {
            return new LamaSkipNode();
        }
        if (ctx.scopedBody() != null) {
            return scopedBodyNode(ctx.scopedBody(), ctx);
        }
        throw fail(ctx, "unsupported expression form");
    }

    @Override
    public LamaExpressionNode visitIfExpression(LamaLanguageParser.IfExpressionContext ctx) {
        LamaExpressionNode result = ctx.ELSE() != null
                        ? visit(ctx.scopeExpression(ctx.scopeExpression().size() - 1))
                        : new LamaSkipNode();

        int branches = ctx.ELIF().size() + 1;
        for (int i = branches - 1; i >= 0; i--) {
            LamaExpressionNode cond = visit(ctx.expression(i));
            LamaExpressionNode body = visit(ctx.scopeExpression(i));
            result = new LamaIfNode(cond, body, result);
        }
        return result;
    }

    @Override
    public LamaExpressionNode visitWhileExpression(LamaLanguageParser.WhileExpressionContext ctx) {
        return new LamaWhileNode(visit(ctx.expression()), visit(ctx.scopeExpression()));
    }

    @Override
    public LamaExpressionNode visitDoWhileExpression(LamaLanguageParser.DoWhileExpressionContext ctx) {
        LamaExpressionNode body = flattenScopedExpressionForSharedEnv(visit(ctx.scopeExpression()));
        return new LamaDoWhileScopedNode(body, visit(ctx.expression()));
    }

    @Override
    public LamaExpressionNode visitForExpression(LamaLanguageParser.ForExpressionContext ctx) {
        LamaExpressionNode init = visit(ctx.forInit());
        LamaExpressionNode cond = visit(ctx.expression(0));
        LamaExpressionNode step = visit(ctx.expression(1));
        LamaExpressionNode body = visit(ctx.scopeExpression());
        return new LamaForNode(init, cond, step, body);
    }

    @Override
    public LamaExpressionNode visitForInit(LamaLanguageParser.ForInitContext ctx) {
        if (ctx.KW_SKIP() != null) {
            return new LamaSkipNode();
        }
        if (ctx.varDefinition() != null) {
            LamaExpressionNode decl = visit(ctx.varDefinition());
            if (ctx.expression() == null) {
                return decl;
            }
            return asSequence(List.of(decl, visit(ctx.expression())));
        }
        return visit(ctx.expression());
    }

    @Override
    public LamaExpressionNode visitScopeExpression(LamaLanguageParser.ScopeExpressionContext ctx) {
        return scopedBodyNode(ctx.scopedBody(), ctx);
    }

    @Override
    public LamaExpressionNode visitCaseExpression(LamaLanguageParser.CaseExpressionContext ctx) {
        LamaExpressionNode scrutinee = visit(ctx.expression());
        LamaPattern[] patterns = new LamaPattern[ctx.caseBranch().size()];
        LamaExpressionNode[] bodies = new LamaExpressionNode[ctx.caseBranch().size()];
        for (int i = 0; i < ctx.caseBranch().size(); i++) {
            patterns[i] = toPattern(ctx.caseBranch(i).pattern());
            bodies[i] = visit(ctx.caseBranch(i).expression());
        }
        return new LamaCaseNode(scrutinee, patterns, bodies);
    }

    @Override
    public LamaExpressionNode visitLetExpression(LamaLanguageParser.LetExpressionContext ctx) {
        LamaPattern pattern = toPattern(ctx.letBinding().pattern());
        LamaExpressionNode value = visit(ctx.letBinding().expression());
        LamaExpressionNode body = visit(ctx.expression());
        return new LamaCaseNode(value, new LamaPattern[]{pattern}, new LamaExpressionNode[]{body});
    }

    @Override
    public LamaExpressionNode visitInfixReference(LamaLanguageParser.InfixReferenceContext ctx) {
        return new LamaReadVarNode(ctx.operatorSymbol().getText());
    }

    @Override
    public LamaExpressionNode visitFunLiteral(LamaLanguageParser.FunLiteralContext ctx) {
        return new LamaLambdaNode(bindParamsFromParameterList(ctx.parameterList()), visit(ctx.blockExpression()));
    }

    @Override
    public LamaExpressionNode visitLiteral(LamaLanguageParser.LiteralContext ctx) {
        if (ctx.INTEGER_LITERAL() != null) {
            return new LamaLongLiteralNode(Long.parseLong(ctx.INTEGER_LITERAL().getText()));
        }
        if (ctx.STRING_LITERAL() != null) {
            return new LamaStringLiteralNode(unquote(ctx.STRING_LITERAL().getText()));
        }
        if (ctx.CHAR_LITERAL() != null) {
            String value = unquote(ctx.CHAR_LITERAL().getText());
            long ch = value.isEmpty() ? 0L : value.charAt(0);
            return new LamaLongLiteralNode(ch);
        }
        if (ctx.TRUE() != null) {
            return new LamaLongLiteralNode(1);
        }
        if (ctx.FALSE() != null) {
            return new LamaLongLiteralNode(0);
        }
        if (ctx.arrayLiteral() != null) {
            return visit(ctx.arrayLiteral());
        }
        return visit(ctx.listLiteral());
    }

    @Override
    public LamaExpressionNode visitArrayLiteral(LamaLanguageParser.ArrayLiteralContext ctx) {
        List<LamaExpressionNode> elements = new ArrayList<>();
        for (var expr : ctx.expression()) {
            elements.add(visit(expr));
        }
        return new LamaArrayLiteralNode(elements.toArray(new LamaExpressionNode[0]));
    }

    @Override
    public LamaExpressionNode visitListLiteral(LamaLanguageParser.ListLiteralContext ctx) {
        LamaExpressionNode list = new LamaSexpLiteralNode("Nil", new LamaExpressionNode[0]);
        List<LamaLanguageParser.ExpressionContext> values = ctx.expression();
        for (int i = values.size() - 1; i >= 0; i--) {
            list = new LamaSexpLiteralNode("Cons", new LamaExpressionNode[]{visit(values.get(i)), list});
        }
        return list;
    }

    private LamaExpressionNode scopedBodyNode(LamaLanguageParser.ScopedBodyContext body, ParserRuleContext fallback) {
        if (body == null || body.scopedItem().isEmpty()) {
            return new LamaLongLiteralNode(0);
        }

        List<LamaExpressionNode> definitions = new ArrayList<>();
        List<LamaExpressionNode> expressions = new ArrayList<>();
        boolean inExpressionPart = false;

        for (var item : body.scopedItem()) {
            LamaExpressionNode node = visit(item);
            boolean isDefinition = item.definition() != null;
            if (!inExpressionPart && isDefinition) {
                definitions.add(node);
            } else {
                if (isDefinition) {
                    throw fail(item, "definitions must appear before expressions in a scope");
                }
                inExpressionPart = true;
                expressions.add(node);
            }
        }

        LamaExpressionNode bodyNode = asSequence(expressions);
        if (definitions.isEmpty()) {
            return bodyNode;
        }
        return new LamaScopeNode(definitions.toArray(new LamaExpressionNode[0]), bodyNode);
    }

    private LamaExpressionNode asSequence(List<LamaExpressionNode> nodes) {
        if (nodes.isEmpty()) {
            return new LamaLongLiteralNode(0);
        }
        if (nodes.size() == 1) {
            return nodes.get(0);
        }
        return new LamaSequenceNode(nodes.toArray(new LamaExpressionNode[0]));
    }

    private LamaExpressionNode flattenScopedExpressionForSharedEnv(LamaExpressionNode node) {
        if (!(node instanceof LamaScopeNode scope)) {
            return node;
        }
        List<LamaExpressionNode> parts = new ArrayList<>();
        for (LamaExpressionNode def : scope.getDefinitions()) {
            parts.add(def);
        }
        parts.add(scope.getBody());
        return asSequence(parts);
    }

    private LamaExpressionNode asLValue(LamaExpressionNode node, ParserRuleContext ctx) {
        if (node instanceof LamaReadVarNode readVar) {
            return new LamaVarLValueNode(readVar.getName());
        }
        if (node instanceof LamaElemReadNode elem) {
            return new LamaElemLValueNode(elem.getReceiver(), elem.getIndex());
        }
        if (node instanceof LamaIfNode ifNode) {
            return new LamaIfNode(
                            ifNode.getCondition(),
                            asLValue(ifNode.getThenBranch(), ctx),
                            asLValue(ifNode.getElseBranch(), ctx));
        }
        if (node instanceof LamaSequenceNode seq) {
            LamaExpressionNode[] parts = seq.getExpressions().clone();
            if (parts.length == 0) {
                throw fail(ctx, "left side of ':=' is not assignable");
            }
            parts[parts.length - 1] = asLValue(parts[parts.length - 1], ctx);
            return new LamaSequenceNode(parts);
        }
        if (node instanceof LamaScopeNode scope) {
            return new LamaScopeNode(scope.getDefinitions(), asLValue(scope.getBody(), ctx));
        }
        throw fail(ctx, "left side of ':=' is not assignable");
    }

    private LamaExpressionNode[] visitArgs(LamaLanguageParser.ArgumentListContext args) {
        if (args == null || args.expression().isEmpty()) {
            return new LamaExpressionNode[0];
        }
        LamaExpressionNode[] values = new LamaExpressionNode[args.expression().size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = visit(args.expression(i));
        }
        return values;
    }

    private List<LamaPattern> bindParamsFromParameterList(LamaLanguageParser.ParameterListContext parameterList) {
        List<LamaPattern> params = new ArrayList<>(parameterList.pattern().size());
        for (var pattern : parameterList.pattern()) {
            params.add(toPattern(pattern));
        }
        return params;
    }

    private LamaPattern toPattern(LamaLanguageParser.PatternContext pattern) {
        return toConsPattern(pattern.consPattern());
    }

    private LamaPattern toConsPattern(LamaLanguageParser.ConsPatternContext pattern) {
        LamaPattern left = toAsPattern(pattern.asPattern());
        if (pattern.consPattern() == null) {
            return left;
        }
        return new LamaSexpPattern("Cons", new LamaPattern[]{left, toConsPattern(pattern.consPattern())});
    }

    private LamaPattern toAsPattern(LamaLanguageParser.AsPatternContext pattern) {
        if (pattern.AT_SIGN() != null) {
            return new LamaAsPattern(pattern.IDENTIFIER().getText(), toAsPattern(pattern.asPattern()));
        }
        return toAtomicPattern(pattern.atomicPattern());
    }

    private LamaPattern toAtomicPattern(LamaLanguageParser.AtomicPatternContext pattern) {
        if (pattern.UNDERSCORE() != null) {
            return new LamaWildcardPattern();
        }
        if (pattern.typePattern() != null) {
            return toTypePattern(pattern.typePattern());
        }
        if (pattern.literalPattern() != null) {
            return toLiteralPattern(pattern.literalPattern());
        }
        if (pattern.LPAREN() != null) {
            return toPattern(pattern.pattern(0));
        }
        if (pattern.LBRACK() != null) {
            LamaPattern[] items = new LamaPattern[pattern.pattern().size()];
            for (int i = 0; i < items.length; i++) {
                items[i] = toPattern(pattern.pattern(i));
            }
            return new LamaArrayPattern(items);
        }
        if (pattern.LBRACE() != null) {
            LamaPattern list = new LamaSexpPattern("Nil", new LamaPattern[0]);
            for (int i = pattern.pattern().size() - 1; i >= 0; i--) {
                list = new LamaSexpPattern("Cons", new LamaPattern[]{toPattern(pattern.pattern(i)), list});
            }
            return list;
        }
        if (pattern.patternArguments() != null || isConstructorName(pattern.IDENTIFIER().getText())) {
            LamaPattern[] args;
            if (pattern.patternArguments() == null) {
                args = new LamaPattern[0];
            } else {
                args = new LamaPattern[pattern.patternArguments().pattern().size()];
                for (int i = 0; i < args.length; i++) {
                    args[i] = toPattern(pattern.patternArguments().pattern(i));
                }
            }
            return new LamaSexpPattern(pattern.IDENTIFIER().getText(), args);
        }
        return new LamaBindPattern(pattern.IDENTIFIER().getText());
    }

    private LamaPattern toTypePattern(LamaLanguageParser.TypePatternContext pattern) {
        if (pattern.IDENTIFIER() != null) {
            return new LamaTypePattern(pattern.IDENTIFIER().getText());
        }
        return new LamaTypePattern("fun");
    }

    private LamaPattern toLiteralPattern(LamaLanguageParser.LiteralPatternContext pattern) {
        if (pattern.INTEGER_LITERAL() != null) {
            return new LamaLongPattern(Long.parseLong(pattern.INTEGER_LITERAL().getText()));
        }
        if (pattern.STRING_LITERAL() != null) {
            return new LamaStringPattern(unquote(pattern.STRING_LITERAL().getText()));
        }
        if (pattern.CHAR_LITERAL() != null) {
            String ch = unquote(pattern.CHAR_LITERAL().getText());
            return new LamaLongPattern(ch.isEmpty() ? 0 : ch.charAt(0));
        }
        return new LamaLongPattern(pattern.TRUE() != null ? 1 : 0);
    }

    private LamaExpressionNode buildBinary(String op, LamaExpressionNode left, LamaExpressionNode right, ParserRuleContext ctx) {
        return switch (op) {
            case ":=" -> new LamaAssignNode(asLValue(left, ctx), right);
            case "+" -> new LamaBinaryNode(LamaBinaryNode.Operator.ADD, left, right);
            case "-" -> new LamaBinaryNode(LamaBinaryNode.Operator.SUB, left, right);
            case "*" -> new LamaBinaryNode(LamaBinaryNode.Operator.MUL, left, right);
            case "/" -> new LamaBinaryNode(LamaBinaryNode.Operator.DIV, left, right);
            case "%" -> new LamaBinaryNode(LamaBinaryNode.Operator.MOD, left, right);
            case "<" -> new LamaBinaryNode(LamaBinaryNode.Operator.LT, left, right);
            case "<=" -> new LamaBinaryNode(LamaBinaryNode.Operator.LE, left, right);
            case ">" -> new LamaBinaryNode(LamaBinaryNode.Operator.GT, left, right);
            case ">=" -> new LamaBinaryNode(LamaBinaryNode.Operator.GE, left, right);
            case "=", "==" -> new LamaBinaryNode(LamaBinaryNode.Operator.EQ, left, right);
            case "!=" -> new LamaBinaryNode(LamaBinaryNode.Operator.NE, left, right);
            case "&&" -> new LamaBinaryNode(LamaBinaryNode.Operator.AND, left, right);
            case "!!", "||" -> new LamaBinaryNode(LamaBinaryNode.Operator.OR, left, right);
            case ":" -> new LamaSexpLiteralNode("Cons", new LamaExpressionNode[]{left, right});
            default -> new LamaCallNode(op, new LamaExpressionNode[]{left, right});
        };
    }

    private static boolean isConstructorName(String name) {
        return !name.isEmpty() && Character.isUpperCase(name.charAt(0));
    }

    private String unquote(String text) {
        String raw = text.substring(1, text.length() - 1);
        StringBuilder out = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c != '\\' || i + 1 >= raw.length()) {
                out.append(c);
                continue;
            }
            char next = raw.charAt(++i);
            switch (next) {
                case 'b' -> out.append('\b');
                case 't' -> out.append('\t');
                case 'n' -> out.append('\n');
                case 'f' -> out.append('\f');
                case 'r' -> out.append('\r');
                case '"', '\'', '\\' -> out.append(next);
                default -> out.append(next);
            }
        }
        return out.toString();
    }

    private LamaParseError fail(ParserRuleContext ctx, String message) {
        Token start = ctx.getStart();
        int line = start != null ? start.getLine() : 1;
        int column = start != null ? start.getCharPositionInLine() + 1 : 1;
        int length = 1;
        if (start != null && start.getStartIndex() >= 0 && start.getStopIndex() >= start.getStartIndex()) {
            length = Math.max(1, start.getStopIndex() - start.getStartIndex() + 1);
        }
        return new LamaParseError(source, line, column, length, message);
    }
}
