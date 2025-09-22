package me.elinge.lox;

import java.util.ArrayList;
import java.util.List;

class Parser {
    private static class ParseError extends RuntimeException {
        private static final long serialVersionUID = -7164347622529507778L;
    }

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        try {
            var statements = new ArrayList<Stmt>();
            while (!isAtEnd()) {
                statements.add(declaration());
            }

            return statements;
        } catch (ParseError error) {
            return null;
        }
    }

    private Expr expression() {
        return assignment();
    }

    private Stmt declaration() {
        try {
            return match(TokenType.VAR) ? varDeclaration() : statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt statement() {
        if (match(TokenType.PRINT)) {
            return printStatement();
        }

        if (match(TokenType.LEFT_BRACE)) {
            return new Stmt.Block(block());
        }

        return  expressionStatement();
    }

    private Stmt printStatement() {
        var value = expression();
        consume(TokenType.SEMICOLON, "Expected ';' after value.");
        return new Stmt.Print(value);
    }

    private Stmt varDeclaration() {
        var name = consume(TokenType.IDENTIFIER, "Expected variable name.");

        Expr initializer = null;
        if (match(TokenType.EQUAL)) {
            initializer = expression();
        }

        consume(TokenType.SEMICOLON, "Expected ';' after variable declaration.");
        return new Stmt.Var(name, initializer);
    }

    private Stmt expressionStatement() {
        var expr = expression();
        consume(TokenType.SEMICOLON, "Expected ';' after expression.");
        return new Stmt.Expression(expr);
    }

    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();

        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(TokenType.RIGHT_BRACE, "Expected '}' after block.");
        return statements;
    }

    private Expr assignment() {
        var expr = checkNotBinaryOperator();

        if (match(TokenType.EQUAL)) {
            var equals = previous();
            var value = assignment();

            if (expr instanceof Expr.Variable) {
                var name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }

            error(equals, "Invalid assignment target.");
        }

        return expr;
    }

    private Expr checkNotBinaryOperator() {
        if (match(
                TokenType.MINUS,
                TokenType.PLUS,
                TokenType.SLASH,
                TokenType.STAR,
                TokenType.BANG_EQUAL,
                TokenType.EQUAL_EQUAL,
                TokenType.GREATER,
                TokenType.GREATER_EQUAL,
                TokenType.LESS,
                TokenType.LESS_EQUAL)) {
            // Discard right-hand operand.
            comma();
            throw error(peek(), "Binary operator without left-hand operand.");
        }

        return comma();
    }

    private Expr comma() {
        var expr = elvis();

        while (match(TokenType.COMMA)) {
            expr = elvis();
        }

        return expr;
    }

    private Expr elvis() {
        var expr = equality();

        if (match(TokenType.QUESTION)) {
            var operator1 = previous();
            var middle = elvis();
            var operator2 = consume(TokenType.COLON, "Expected ':' after '?' in elvis operator.");
            var right = elvis();
            expr = new Expr.Ternary(expr, operator1, middle, operator2, right);
        }

        return expr;
    }

    private Expr equality() {
        var expr = comparison();

        while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
            var operator = previous();
            var right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr comparison() {
        var expr = term();

        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            var operator = previous();
            var right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr term() {
        var expr = factor();

        while (match(TokenType.MINUS, TokenType.PLUS)) {
            var operator = previous();
            var right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr factor() {
        var expr = unary();

        while (match(TokenType.SLASH, TokenType.STAR)) {
            var operator = previous();
            var right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr unary() {
        if (match(TokenType.BANG, TokenType.MINUS)) {
            var operator = previous();
            var right = unary();
            return new Expr.Unary(operator, right);
        }

        return primary();
    }

    private Expr primary() {
        if (match(TokenType.FALSE)) {
            return new Expr.Literal(false);
        }

        if (match(TokenType.TRUE)) {
            return new Expr.Literal(true);
        }

        if (match(TokenType.NIL)) {
            return new Expr.Literal(null);
        }

        if (match(TokenType.NUMBER, TokenType.STRING)) {
            return new Expr.Literal(previous().literal());
        }

        if (match(TokenType.IDENTIFIER)) {
            return new Expr.Variable(previous());
        }

        if (match(TokenType.LEFT_PAREN)) {
            var expr = expression();
            consume(TokenType.RIGHT_PAREN, "Expected ')' after expression.");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expected expression.");
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }

        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) {
            return advance();
        }

        throw error(peek(), message);
    }

    private boolean check(TokenType type) {
        return !isAtEnd() && peek().type() == type;
    }

    private Token advance() {
        if (!isAtEnd()) {
            current++;
        }

        return previous();
    }

    private boolean isAtEnd() {
        return peek().type() == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    @SuppressWarnings("unused")
    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type() == TokenType.SEMICOLON) {
                return;
            }

            switch (peek().type()) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
                default:
                    break;
            }

            advance();
        }
    }
}
