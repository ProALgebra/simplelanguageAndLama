package com.oracle.truffle.lama.parser;

import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.lama.nodes.LamaAssignNode;
import com.oracle.truffle.lama.nodes.LamaArrayLiteralNode;
import com.oracle.truffle.lama.nodes.LamaBinaryNode;
import com.oracle.truffle.lama.nodes.LamaCallNode;
import com.oracle.truffle.lama.nodes.LamaCaseNode;
import com.oracle.truffle.lama.nodes.LamaDoWhileNode;
import com.oracle.truffle.lama.nodes.LamaDoWhileScopedNode;
import com.oracle.truffle.lama.nodes.LamaExpressionNode;
import com.oracle.truffle.lama.nodes.LamaForNode;
import com.oracle.truffle.lama.nodes.LamaIfNode;
import com.oracle.truffle.lama.nodes.LamaIndirectCallNode;
import com.oracle.truffle.lama.nodes.LamaFunctionDefNode;
import com.oracle.truffle.lama.nodes.LamaLambdaNode;
import com.oracle.truffle.lama.nodes.LamaElemLValueNode;
import com.oracle.truffle.lama.nodes.LamaElemReadNode;
import com.oracle.truffle.lama.nodes.LamaLongLiteralNode;
import com.oracle.truffle.lama.nodes.LamaReadVarNode;
import com.oracle.truffle.lama.nodes.LamaScopeNode;
import com.oracle.truffle.lama.nodes.LamaSequenceNode;
import com.oracle.truffle.lama.nodes.LamaSkipNode;
import com.oracle.truffle.lama.nodes.LamaSexpLiteralNode;
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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class LamaParser {
    private enum Assoc {
        LEFT,
        RIGHT,
        NONA,
    }

    private record OpInfo(int precedence, Assoc assoc) {
    }

    private static final Map<String, OpInfo> OPS = new HashMap<>();

    static {
        register(0, Assoc.RIGHT, ":=");
        register(1, Assoc.RIGHT, ":");
        register(2, Assoc.LEFT, "!!");
        register(3, Assoc.LEFT, "&&");
        register(4, Assoc.NONA, "=", "==", "!=", "<=", "<", ">=", ">");
        register(5, Assoc.LEFT, "+", "-");
        register(6, Assoc.LEFT, "*", "/", "%");
    }

    private static void register(int precedence, Assoc assoc, String... ops) {
        for (String op : ops) {
            OPS.put(op, new OpInfo(precedence, assoc));
        }
    }

    private final List<LamaToken> tokens;
    private final Source source;
    private int pos;

    LamaParser(List<LamaToken> tokens, Source source) {
        this.tokens = tokens;
        this.source = source;
    }

    LamaExpressionNode parseProgram() {
        List<LamaExpressionNode> statements = new ArrayList<>();
        while (!check(LamaTokenType.EOF)) {
            if (match(LamaTokenType.SEMI)) {
                continue;
            }
            statements.add(parseStmtOrExpr(EnumSet.of(LamaTokenType.EOF)));
            match(LamaTokenType.SEMI);
        }
        if (statements.isEmpty()) {
            return new LamaLongLiteralNode(0);
        }
        if (statements.size() == 1) {
            return statements.get(0);
        }
        return new LamaSequenceNode(statements.toArray(new LamaExpressionNode[0]));
    }

    private LamaExpressionNode parseBlock(Set<LamaTokenType> terminators) {
        List<LamaExpressionNode> statements = new ArrayList<>();
        while (!checkAny(terminators)) {
            if (match(LamaTokenType.SEMI)) {
                continue;
            }
            statements.add(parseStmtOrExpr(terminators));
            match(LamaTokenType.SEMI);
        }
        if (statements.isEmpty()) {
            return new LamaLongLiteralNode(0);
        }
        if (statements.size() == 1) {
            return statements.get(0);
        }
        return new LamaSequenceNode(statements.toArray(new LamaExpressionNode[0]));
    }

    private LamaExpressionNode parseStmtOrExpr(Set<LamaTokenType> terminators) {
        if (checkAny(terminators)) {
            return new LamaLongLiteralNode(0);
        }
        if (check(LamaTokenType.KW_VAR)) {
            return parseVarDecl();
        }
        if (check(LamaTokenType.KW_FUN) && peekNextIsName()) {
            return parseFunctionDef();
        }
        if (peekNextIsInfixDefinition()) {
            return parseInfixDef();
        }
        return parseExpression(0, terminators);
    }

    private LamaExpressionNode parseScope(Set<LamaTokenType> terminators) {
        List<LamaExpressionNode> definitions = new ArrayList<>();
        while (!checkAny(terminators)) {
            if (match(LamaTokenType.SEMI)) {
                continue;
            }
            if (check(LamaTokenType.KW_VAR)) {
                definitions.add(parseVarDecl());
                match(LamaTokenType.SEMI);
            } else if (check(LamaTokenType.KW_FUN) && peekNextIsName()) {
                definitions.add(parseFunctionDef());
                match(LamaTokenType.SEMI);
            } else if (peekNextIsInfixDefinition()) {
                definitions.add(parseInfixDef());
                match(LamaTokenType.SEMI);
            } else {
                break;
            }
        }

        List<LamaExpressionNode> body = new ArrayList<>();
        while (!checkAny(terminators)) {
            if (match(LamaTokenType.SEMI)) {
                continue;
            }
            body.add(parseExpression(0, terminators));
            match(LamaTokenType.SEMI);
        }

        LamaExpressionNode bodyNode;
        if (body.isEmpty()) {
            bodyNode = new LamaLongLiteralNode(0);
        } else if (body.size() == 1) {
            bodyNode = body.get(0);
        } else {
            bodyNode = new LamaSequenceNode(body.toArray(new LamaExpressionNode[0]));
        }

        if (definitions.isEmpty()) {
            return bodyNode;
        }
        return new LamaScopeNode(definitions.toArray(new LamaExpressionNode[0]), bodyNode);
    }

    private LamaVarDeclNode parseVarDecl() {
        consume(LamaTokenType.KW_VAR, "Expected 'var'");
        List<LamaVarDeclNode.Decl> decls = new ArrayList<>();
        do {
            LamaToken name = consumeAny(EnumSet.of(LamaTokenType.IDENT, LamaTokenType.UIDENT), "Expected identifier");
            LamaExpressionNode init = null;
            if (matchOp("=")) {
                init = parseExpression(0, EnumSet.of(LamaTokenType.COMMA, LamaTokenType.SEMI, LamaTokenType.EOF, LamaTokenType.RPAREN, LamaTokenType.RBRACE, LamaTokenType.RBRACK));
            }
            decls.add(new LamaVarDeclNode.Decl(name.text, init));
        } while (match(LamaTokenType.COMMA));
        return new LamaVarDeclNode(decls);
    }

    private LamaExpressionNode parseExpression(int minPrecedence, Set<LamaTokenType> terminators) {
        LamaExpressionNode left = parsePrefix(terminators);

        while (true) {
            if (checkAny(terminators)) {
                break;
            }
            if (!check(LamaTokenType.OP)) {
                break;
            }
            String opText = peek().text;
            OpInfo info = OPS.get(opText);
            if (info == null) {
                break;
            }
            if (info.precedence < minPrecedence) {
                break;
            }
            advance(); // consume operator

            int nextMinPrec = switch (info.assoc) {
                case LEFT, NONA -> info.precedence + 1;
                case RIGHT -> info.precedence;
            };

            LamaExpressionNode right = parseExpression(nextMinPrec, terminators);
            left = buildBinary(opText, left, right);
        }
        return left;
    }

    private LamaExpressionNode parsePrefix(Set<LamaTokenType> terminators) {
        if (checkAny(terminators)) {
            return new LamaLongLiteralNode(0);
        }

        if (check(LamaTokenType.IDENT) && "eta".equals(peek().text)) {
            advance();
            return parsePrefix(terminators);
        }

        if (match(LamaTokenType.KW_INFIX)) {
            LamaToken op = consume(LamaTokenType.OP, "Expected operator after 'infix'");
            consume(LamaTokenType.LPAREN, "Expected '('");
            List<LamaExpressionNode> args = new ArrayList<>();
            if (!check(LamaTokenType.RPAREN)) {
                do {
                    args.add(parseExpression(0, EnumSet.of(LamaTokenType.COMMA, LamaTokenType.RPAREN)));
                } while (match(LamaTokenType.COMMA));
            }
            consume(LamaTokenType.RPAREN, "Expected ')'");
            return new LamaCallNode(op.text, args.toArray(new LamaExpressionNode[0]));
        }

        if (match(LamaTokenType.KW_CASE)) {
            LamaExpressionNode scrutinee = parseExpression(0, EnumSet.of(LamaTokenType.KW_OF));
            consume(LamaTokenType.KW_OF, "Expected 'of'");
            List<LamaPattern> patterns = new ArrayList<>();
            List<LamaExpressionNode> bodies = new ArrayList<>();
            boolean first = true;
            while (!check(LamaTokenType.KW_ESAC)) {
                if (first) {
                    match(LamaTokenType.PIPE);
                    first = false;
                } else {
                    consume(LamaTokenType.PIPE, "Expected '|'");
                }
                patterns.add(parsePattern(EnumSet.of(LamaTokenType.ARROW)));
                consume(LamaTokenType.ARROW, "Expected '->'");
                bodies.add(parseScope(EnumSet.of(LamaTokenType.PIPE, LamaTokenType.KW_ESAC)));
            }
            consume(LamaTokenType.KW_ESAC, "Expected 'esac'");
            return new LamaCaseNode(scrutinee, patterns.toArray(new LamaPattern[0]), bodies.toArray(new LamaExpressionNode[0]));
        }

        if (match(LamaTokenType.KW_LET)) {
            LamaPattern pattern = parsePattern(EnumSet.of(LamaTokenType.OP));
            LamaToken eq = consume(LamaTokenType.OP, "Expected '=' in let binding");
            if (!"=".equals(eq.text)) {
                throw error(eq, "Expected '=' in let binding");
            }
            LamaExpressionNode value = parseExpression(0, EnumSet.of(LamaTokenType.KW_IN));
            consume(LamaTokenType.KW_IN, "Expected 'in' in let expression");
            LamaExpressionNode body = parseScope(terminators);
            return new LamaCaseNode(value, new LamaPattern[]{pattern}, new LamaExpressionNode[]{body});
        }

        if (match(LamaTokenType.KW_IF)) {
            LamaExpressionNode condition = parseExpression(0, EnumSet.of(LamaTokenType.KW_THEN));
            consume(LamaTokenType.KW_THEN, "Expected 'then'");
            LamaExpressionNode thenBranch = parseScope(EnumSet.of(LamaTokenType.KW_ELIF, LamaTokenType.KW_ELSE, LamaTokenType.KW_FI));
            List<LamaExpressionNode> elifConds = new ArrayList<>();
            List<LamaExpressionNode> elifBodies = new ArrayList<>();
            while (match(LamaTokenType.KW_ELIF)) {
                elifConds.add(parseExpression(0, EnumSet.of(LamaTokenType.KW_THEN)));
                consume(LamaTokenType.KW_THEN, "Expected 'then'");
                elifBodies.add(parseScope(EnumSet.of(LamaTokenType.KW_ELIF, LamaTokenType.KW_ELSE, LamaTokenType.KW_FI)));
            }
            LamaExpressionNode elseBranch;
            if (match(LamaTokenType.KW_ELSE)) {
                elseBranch = parseScope(EnumSet.of(LamaTokenType.KW_FI));
            } else {
                elseBranch = new LamaSkipNode();
            }
            consume(LamaTokenType.KW_FI, "Expected 'fi'");

            LamaExpressionNode result = new LamaIfNode(condition, thenBranch, elseBranch);
            for (int i = elifConds.size() - 1; i >= 0; i--) {
                result = new LamaIfNode(elifConds.get(i), elifBodies.get(i), result);
            }
            return result;
        }

        if (match(LamaTokenType.KW_WHILE)) {
            // Lama allows sequencing in the condition (e.g. "while a := ...; a < 10 do ... od").
            LamaExpressionNode condition = parseScope(EnumSet.of(LamaTokenType.KW_DO));
            consume(LamaTokenType.KW_DO, "Expected 'do'");
            LamaExpressionNode body = parseScope(EnumSet.of(LamaTokenType.KW_OD));
            consume(LamaTokenType.KW_OD, "Expected 'od'");
            return new LamaWhileNode(condition, body);
        }

        if (match(LamaTokenType.KW_DO)) {
            LamaExpressionNode body = parseBlock(EnumSet.of(LamaTokenType.KW_WHILE));
            consume(LamaTokenType.KW_WHILE, "Expected 'while'");
            LamaExpressionNode condition = parseBlock(EnumSet.of(LamaTokenType.KW_OD));
            consume(LamaTokenType.KW_OD, "Expected 'od'");
            return new LamaDoWhileScopedNode(body, condition);
        }

        if (match(LamaTokenType.KW_FOR)) {
            LamaExpressionNode varDecl = null;
            if (check(LamaTokenType.KW_VAR)) {
                varDecl = parseVarDecl();
                consume(LamaTokenType.SEMI, "Expected ';' after for-loop variable declarations");
            }

            LamaExpressionNode init = parseExpression(0, EnumSet.of(LamaTokenType.COMMA));
            consume(LamaTokenType.COMMA, "Expected ','");
            LamaExpressionNode condition = parseExpression(0, EnumSet.of(LamaTokenType.COMMA));
            consume(LamaTokenType.COMMA, "Expected ','");
            LamaExpressionNode step = parseExpression(0, EnumSet.of(LamaTokenType.KW_DO));
            consume(LamaTokenType.KW_DO, "Expected 'do'");
            LamaExpressionNode body = parseScope(EnumSet.of(LamaTokenType.KW_OD));
            consume(LamaTokenType.KW_OD, "Expected 'od'");
            LamaExpressionNode loop = new LamaForNode(init, condition, step, body);
            if (varDecl != null) {
                return new LamaScopeNode(new LamaExpressionNode[]{varDecl}, loop);
            }
            return loop;
        }

        if (match(LamaTokenType.KW_SKIP)) {
            return new LamaSkipNode();
        }

        if (match(LamaTokenType.KW_TRUE)) {
            return new LamaLongLiteralNode(1);
        }
        if (match(LamaTokenType.KW_FALSE)) {
            return new LamaLongLiteralNode(0);
        }

        if (matchOp("+")) {
            return parsePrefix(terminators);
        }
        if (matchOp("-")) {
            return new LamaUnaryNode(LamaUnaryNode.Operator.MINUS, parsePrefix(terminators));
        }

        if (match(LamaTokenType.KW_FUN)) {
            consume(LamaTokenType.LPAREN, "Expected '(' after 'fun'");
            List<LamaPattern> params = new ArrayList<>();
            if (!check(LamaTokenType.RPAREN)) {
                do {
                    params.add(parsePattern(EnumSet.of(LamaTokenType.COMMA, LamaTokenType.RPAREN)));
                } while (match(LamaTokenType.COMMA));
            }
            consume(LamaTokenType.RPAREN, "Expected ')'");
            consume(LamaTokenType.LBRACE, "Expected '{'");
            LamaExpressionNode body = parseScope(EnumSet.of(LamaTokenType.RBRACE));
            consume(LamaTokenType.RBRACE, "Expected '}'");
            return new LamaLambdaNode(params, body);
        }

        LamaExpressionNode primary = parsePrimary(terminators);
        return parsePostfix(primary, terminators);
    }

    private LamaExpressionNode parsePrimary(Set<LamaTokenType> terminators) {
        if (checkAny(terminators)) {
            return new LamaLongLiteralNode(0);
        }
        if (match(LamaTokenType.INT)) {
            return new LamaLongLiteralNode(Long.parseLong(previous().text));
        }
        if (match(LamaTokenType.STRING)) {
            return new LamaStringLiteralNode(previous().text);
        }
        if (match(LamaTokenType.CHAR)) {
            return new LamaLongLiteralNode(previous().text.charAt(0));
        }
        if (match(LamaTokenType.LBRACE)) {
            // list literal: {} or {a, b, c} desugars to Cons(a, Cons(b, Cons(c, Nil)))
            List<LamaExpressionNode> elems = new ArrayList<>();
            if (!check(LamaTokenType.RBRACE)) {
                do {
                    elems.add(parseExpression(0, EnumSet.of(LamaTokenType.COMMA, LamaTokenType.RBRACE)));
                } while (match(LamaTokenType.COMMA));
            }
            consume(LamaTokenType.RBRACE, "Expected '}'");
            LamaExpressionNode list = new LamaSexpLiteralNode("Nil", new LamaExpressionNode[0]);
            for (int i = elems.size() - 1; i >= 0; i--) {
                list = new LamaSexpLiteralNode("Cons", new LamaExpressionNode[]{elems.get(i), list});
            }
            return list;
        }
        if (match(LamaTokenType.LBRACK)) {
            List<LamaExpressionNode> elems = new ArrayList<>();
            if (!check(LamaTokenType.RBRACK)) {
                do {
                    elems.add(parseExpression(0, EnumSet.of(LamaTokenType.COMMA, LamaTokenType.RBRACK)));
                } while (match(LamaTokenType.COMMA));
            }
            consume(LamaTokenType.RBRACK, "Expected ']'");
            return new LamaArrayLiteralNode(elems.toArray(new LamaExpressionNode[0]));
        }
        if (match(LamaTokenType.LPAREN)) {
            LamaExpressionNode inner = parseScope(EnumSet.of(LamaTokenType.RPAREN));
            consume(LamaTokenType.RPAREN, "Expected ')'");
            if (inner instanceof LamaScopeNode) {
                return inner;
            }
            return new LamaScopeNode(new LamaExpressionNode[0], inner);
        }
        if (match(LamaTokenType.UIDENT)) {
            LamaToken name = previous();
            List<LamaExpressionNode> fields = new ArrayList<>();
            if (match(LamaTokenType.LPAREN)) {
                if (!check(LamaTokenType.RPAREN)) {
                    do {
                        fields.add(parseExpression(0, EnumSet.of(LamaTokenType.COMMA, LamaTokenType.RPAREN)));
                    } while (match(LamaTokenType.COMMA));
                }
                consume(LamaTokenType.RPAREN, "Expected ')'");
            }
            return new LamaSexpLiteralNode(name.text, fields.toArray(new LamaExpressionNode[0]));
        }
        if (match(LamaTokenType.IDENT)) {
            LamaToken name = previous();
            if (check(LamaTokenType.LPAREN)) {
                consume(LamaTokenType.LPAREN, "Expected '('");
                List<LamaExpressionNode> args = new ArrayList<>();
                if (!check(LamaTokenType.RPAREN)) {
                    do {
                        args.add(parseExpression(0, EnumSet.of(LamaTokenType.COMMA, LamaTokenType.RPAREN)));
                    } while (match(LamaTokenType.COMMA));
                }
                consume(LamaTokenType.RPAREN, "Expected ')'");
                return new LamaCallNode(name.text, args.toArray(new LamaExpressionNode[0]));
            }
            return new LamaReadVarNode(name.text);
        }
        throw error(peek(), "Unexpected token");
    }

    private LamaExpressionNode parseFunctionDef() {
        consume(LamaTokenType.KW_FUN, "Expected 'fun'");
        LamaToken nameToken = consumeAny(EnumSet.of(LamaTokenType.IDENT, LamaTokenType.UIDENT), "Expected function name");
        consume(LamaTokenType.LPAREN, "Expected '('");
        List<LamaPattern> params = new ArrayList<>();
        if (!check(LamaTokenType.RPAREN)) {
            do {
                params.add(parsePattern(EnumSet.of(LamaTokenType.COMMA, LamaTokenType.RPAREN)));
            } while (match(LamaTokenType.COMMA));
        }
        consume(LamaTokenType.RPAREN, "Expected ')'");
        consume(LamaTokenType.LBRACE, "Expected '{'");
        LamaExpressionNode body = parseScope(EnumSet.of(LamaTokenType.RBRACE));
        consume(LamaTokenType.RBRACE, "Expected '}'");
        return new LamaFunctionDefNode(nameToken.text, params, body);
    }

    private LamaExpressionNode parseInfixDef() {
        LamaTokenType kw = advance().type;
        Assoc assoc = switch (kw) {
            case KW_INFIX -> Assoc.NONA;
            case KW_INFIXL -> Assoc.LEFT;
            case KW_INFIXR -> Assoc.RIGHT;
            default -> throw error(previous(), "Expected infix declaration");
        };

        LamaToken opToken = consume(LamaTokenType.OP, "Expected operator name");
        LamaToken relTok = consumeAny(EnumSet.of(LamaTokenType.KW_AT_KW, LamaTokenType.KW_BEFORE, LamaTokenType.KW_AFTER),
                        "Expected 'at', 'before' or 'after'");
        LamaToken baseOp = consume(LamaTokenType.OP, "Expected base operator");
        OpInfo baseInfo = OPS.get(baseOp.text);
        if (baseInfo == null) {
            throw error(baseOp, "Unknown base operator: " + baseOp.text);
        }
        int precedence = switch (relTok.type) {
            case KW_AT_KW -> baseInfo.precedence;
            case KW_BEFORE -> baseInfo.precedence + 1;
            case KW_AFTER -> Math.max(0, baseInfo.precedence - 1);
            default -> throw error(relTok, "Unexpected token");
        };
        OPS.put(opToken.text, new OpInfo(precedence, assoc));

        consume(LamaTokenType.LPAREN, "Expected '('");
        List<LamaPattern> params = new ArrayList<>();
        if (!check(LamaTokenType.RPAREN)) {
            do {
                params.add(parsePattern(EnumSet.of(LamaTokenType.COMMA, LamaTokenType.RPAREN)));
            } while (match(LamaTokenType.COMMA));
        }
        consume(LamaTokenType.RPAREN, "Expected ')'");
        consume(LamaTokenType.LBRACE, "Expected '{'");
        LamaExpressionNode body = parseScope(EnumSet.of(LamaTokenType.RBRACE));
        consume(LamaTokenType.RBRACE, "Expected '}'");
        return new LamaFunctionDefNode(opToken.text, params, body);
    }

    private LamaExpressionNode parsePostfix(LamaExpressionNode expr, Set<LamaTokenType> terminators) {
        while (!checkAny(terminators)) {
            if (match(LamaTokenType.LPAREN)) {
                List<LamaExpressionNode> args = new ArrayList<>();
                if (!check(LamaTokenType.RPAREN)) {
                    do {
                        args.add(parseExpression(0, EnumSet.of(LamaTokenType.COMMA, LamaTokenType.RPAREN)));
                    } while (match(LamaTokenType.COMMA));
                }
                consume(LamaTokenType.RPAREN, "Expected ')'");
                expr = new LamaIndirectCallNode(expr, args.toArray(new LamaExpressionNode[0]));
            } else if (match(LamaTokenType.DOT)) {
                LamaToken member = consumeAny(EnumSet.of(LamaTokenType.IDENT, LamaTokenType.UIDENT), "Expected member name");
                List<LamaExpressionNode> args = new ArrayList<>();
                args.add(expr);
                if (match(LamaTokenType.LPAREN)) {
                    if (!check(LamaTokenType.RPAREN)) {
                        do {
                            args.add(parseExpression(0, EnumSet.of(LamaTokenType.COMMA, LamaTokenType.RPAREN)));
                        } while (match(LamaTokenType.COMMA));
                    }
                    consume(LamaTokenType.RPAREN, "Expected ')'");
                }
                expr = new LamaCallNode(member.text, args.toArray(new LamaExpressionNode[0]));
            } else if (match(LamaTokenType.LBRACK)) {
                LamaExpressionNode idx = parseExpression(0, EnumSet.of(LamaTokenType.RBRACK));
                consume(LamaTokenType.RBRACK, "Expected ']'");
                expr = new LamaElemReadNode(expr, idx);
            } else {
                break;
            }
        }
        return expr;
    }

    private LamaExpressionNode buildBinary(String op, LamaExpressionNode left, LamaExpressionNode right) {
        return switch (op) {
            case ":=" -> new LamaAssignNode(toLValue(left), right);
            case "+" -> new LamaBinaryNode(LamaBinaryNode.Operator.ADD, left, right);
            case "-" -> new LamaBinaryNode(LamaBinaryNode.Operator.SUB, left, right);
            case "*" -> new LamaBinaryNode(LamaBinaryNode.Operator.MUL, left, right);
            case "/" -> new LamaBinaryNode(LamaBinaryNode.Operator.DIV, left, right);
            case "%" -> new LamaBinaryNode(LamaBinaryNode.Operator.MOD, left, right);
            case "<" -> new LamaBinaryNode(LamaBinaryNode.Operator.LT, left, right);
            case "<=" -> new LamaBinaryNode(LamaBinaryNode.Operator.LE, left, right);
            case ">" -> new LamaBinaryNode(LamaBinaryNode.Operator.GT, left, right);
            case ">=" -> new LamaBinaryNode(LamaBinaryNode.Operator.GE, left, right);
            case "=" -> new LamaBinaryNode(LamaBinaryNode.Operator.EQ, left, right);
            case "==" -> new LamaBinaryNode(LamaBinaryNode.Operator.EQ, left, right);
            case "!=" -> new LamaBinaryNode(LamaBinaryNode.Operator.NE, left, right);
            case "&&" -> new LamaBinaryNode(LamaBinaryNode.Operator.AND, left, right);
            case "!!" -> new LamaBinaryNode(LamaBinaryNode.Operator.OR, left, right);
            case ":" -> new LamaSexpLiteralNode("Cons", new LamaExpressionNode[]{left, right});
            default -> new LamaCallNode(op, new LamaExpressionNode[]{left, right});
        };
    }

    private LamaExpressionNode toLValue(LamaExpressionNode expr) {
        if (expr instanceof LamaVarLValueNode) {
            return expr;
        }
        if (expr instanceof LamaReadVarNode read) {
            return new LamaVarLValueNode(read.getName());
        }
        if (expr instanceof LamaElemReadNode elem) {
            return new LamaElemLValueNode(elem.getReceiver(), elem.getIndex());
        }
        if (expr instanceof LamaIfNode ifNode) {
            return new LamaIfNode(ifNode.getCondition(), toLValue(ifNode.getThenBranch()), toLValue(ifNode.getElseBranch()));
        }
        if (expr instanceof LamaSequenceNode seq) {
            LamaExpressionNode[] exprs = seq.getExpressions();
            if (exprs.length == 0) {
                throw new RuntimeException("Empty sequence is not assignable");
            }
            LamaExpressionNode[] rewritten = exprs.clone();
            rewritten[rewritten.length - 1] = toLValue(rewritten[rewritten.length - 1]);
            return new LamaSequenceNode(rewritten);
        }
        if (expr instanceof LamaScopeNode scope) {
            return new LamaScopeNode(scope.getDefinitions(), toLValue(scope.getBody()));
        }
        throw new RuntimeException("Expression is not assignable");
    }

    // ---------------- token helpers ----------------

    private boolean match(LamaTokenType type) {
        if (check(type)) {
            advance();
            return true;
        }
        return false;
    }

    private boolean matchAny(Set<LamaTokenType> types) {
        if (types.contains(peek().type)) {
            advance();
            return true;
        }
        return false;
    }

    private boolean matchOp(String op) {
        if (check(LamaTokenType.OP) && op.equals(peek().text)) {
            advance();
            return true;
        }
        return false;
    }

    private boolean check(LamaTokenType type) {
        return peek().type == type;
    }

    private boolean checkAny(Set<LamaTokenType> types) {
        return types.contains(peek().type);
    }

    private LamaToken consume(LamaTokenType type, String message) {
        if (check(type)) {
            return advance();
        }
        throw error(peek(), message);
    }

    private LamaToken consumeAny(Set<LamaTokenType> types, String message) {
        if (types.contains(peek().type)) {
            return advance();
        }
        throw error(peek(), message);
    }

    private LamaToken advance() {
        if (!check(LamaTokenType.EOF)) {
            pos++;
        }
        return previous();
    }

    private LamaToken peek() {
        return tokens.get(pos);
    }

    // ---------------- pattern parsing ----------------

    private LamaPattern parsePattern(Set<LamaTokenType> terminators) {
        if (checkAny(terminators)) {
            throw error(peek(), "Expected pattern");
        }

        if (check(LamaTokenType.IDENT) && peekNextTypeIs(LamaTokenType.AT)) {
            LamaToken name = advance();
            consume(LamaTokenType.AT, "Expected '@'");
            LamaPattern inner = parsePattern(terminators);
            return new LamaAsPattern(name.text, inner);
        }

        LamaPattern left = parsePatternAtom(terminators);
        if (matchOp(":")) {
            LamaPattern right = parsePattern(terminators); // right-associative
            return new LamaSexpPattern("Cons", new LamaPattern[]{left, right});
        }
        return left;
    }

    private LamaPattern parsePatternAtom(Set<LamaTokenType> terminators) {
        if (checkAny(terminators)) {
            throw error(peek(), "Expected pattern");
        }

        if (match(LamaTokenType.INT)) {
            return new LamaLongPattern(Long.parseLong(previous().text));
        }
        if (match(LamaTokenType.STRING)) {
            return new LamaStringPattern(previous().text);
        }
        if (match(LamaTokenType.CHAR)) {
            return new LamaLongPattern(previous().text.charAt(0));
        }
        if (match(LamaTokenType.KW_TRUE)) {
            return new LamaLongPattern(1);
        }
        if (match(LamaTokenType.KW_FALSE)) {
            return new LamaLongPattern(0);
        }

        if (match(LamaTokenType.HASH)) {
            LamaToken kind = consumeAny(EnumSet.of(LamaTokenType.IDENT, LamaTokenType.UIDENT, LamaTokenType.KW_FUN), "Expected type name after '#'");
            return new LamaTypePattern(kind.text);
        }

        if (match(LamaTokenType.LPAREN)) {
            LamaPattern inner = parsePattern(EnumSet.of(LamaTokenType.RPAREN));
            consume(LamaTokenType.RPAREN, "Expected ')'");
            return inner;
        }

        if (match(LamaTokenType.LBRACK)) {
            List<LamaPattern> elems = new ArrayList<>();
            if (!check(LamaTokenType.RBRACK)) {
                do {
                    elems.add(parsePattern(EnumSet.of(LamaTokenType.COMMA, LamaTokenType.RBRACK)));
                } while (match(LamaTokenType.COMMA));
            }
            consume(LamaTokenType.RBRACK, "Expected ']'");
            return new LamaArrayPattern(elems.toArray(new LamaPattern[0]));
        }

        if (match(LamaTokenType.LBRACE)) {
            // list pattern: {} or {a, b, c}
            List<LamaPattern> elems = new ArrayList<>();
            if (!check(LamaTokenType.RBRACE)) {
                do {
                    elems.add(parsePattern(EnumSet.of(LamaTokenType.COMMA, LamaTokenType.RBRACE)));
                } while (match(LamaTokenType.COMMA));
            }
            consume(LamaTokenType.RBRACE, "Expected '}'");
            LamaPattern list = new LamaSexpPattern("Nil", new LamaPattern[0]);
            for (int i = elems.size() - 1; i >= 0; i--) {
                list = new LamaSexpPattern("Cons", new LamaPattern[]{elems.get(i), list});
            }
            return list;
        }

        if (match(LamaTokenType.UIDENT)) {
            LamaToken tag = previous();
            List<LamaPattern> fields = new ArrayList<>();
            if (match(LamaTokenType.LPAREN)) {
                if (!check(LamaTokenType.RPAREN)) {
                    do {
                        fields.add(parsePattern(EnumSet.of(LamaTokenType.COMMA, LamaTokenType.RPAREN)));
                    } while (match(LamaTokenType.COMMA));
                }
                consume(LamaTokenType.RPAREN, "Expected ')'");
            }
            return new LamaSexpPattern(tag.text, fields.toArray(new LamaPattern[0]));
        }

        if (match(LamaTokenType.IDENT)) {
            LamaToken name = previous();
            if ("_".equals(name.text)) {
                return new LamaWildcardPattern();
            }
            return new LamaBindPattern(name.text);
        }

        throw error(peek(), "Unexpected token in pattern");
    }

    private boolean peekNextTypeIs(LamaTokenType type) {
        if (pos + 1 >= tokens.size()) {
            return false;
        }
        return tokens.get(pos + 1).type == type;
    }

    private LamaToken peekNext() {
        int i = Math.min(pos + 1, tokens.size() - 1);
        return tokens.get(i);
    }

    private boolean peekNextIsName() {
        LamaTokenType t = peekNext().type;
        return t == LamaTokenType.IDENT || t == LamaTokenType.UIDENT;
    }

    private boolean peekNextIsInfixDefinition() {
        if (!checkAny(EnumSet.of(LamaTokenType.KW_INFIX, LamaTokenType.KW_INFIXL, LamaTokenType.KW_INFIXR))) {
            return false;
        }
        if (pos + 2 >= tokens.size()) {
            return false;
        }
        LamaTokenType t1 = tokens.get(pos + 1).type;
        LamaTokenType t2 = tokens.get(pos + 2).type;
        return t1 == LamaTokenType.OP && (t2 == LamaTokenType.KW_AT_KW || t2 == LamaTokenType.KW_BEFORE || t2 == LamaTokenType.KW_AFTER);
    }

    private LamaToken previous() {
        return tokens.get(pos - 1);
    }

    private RuntimeException error(LamaToken token, String message) {
        throw new LamaParseError(source, token.line, token.column, Math.max(token.text.length(), 1), message);
    }
}
