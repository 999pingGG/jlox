package me.elinge.lox;

// Reverse Polish Notation
class Rpn implements Expr.Visitor<String> {
    String print(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return String.format(
                "%s %s %s",
                expr.left.accept(this),
                expr.right.accept(this),
                expr.operator.lexeme());
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return expr.expression.accept(this);
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        return expr.value == null ? "nil" : expr.value.toString();
    }

    @Override
    public String visitLogicalExpr(Expr.Logical expr) {
        return String.format(
                "%s %s %s",
                expr.left.accept(this),
                expr.right.accept(this),
                expr.operator.lexeme());
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return String.format(
                "%s%s",
                expr.right.accept(this),
                expr.operator.lexeme());
    }

    @Override
    public String visitTernaryExpr(Expr.Ternary expr) {
        return String.format(
                "%s %s %s %s%s",
                expr.left.accept(this),
                expr.middle.accept(this),
                expr.right.accept(this),
                expr.operator1.lexeme(),
                expr.operator2.lexeme());
    }

    @Override
    public String visitAssignExpr(Expr.Assign expr) {
        return expr.name + " " + expr.value + " =";
    }

    @Override
    public String visitVariableExpr(Expr.Variable expr) {
        return expr.name.lexeme();
    }

    public static void main(String[] args) {
        Expr expression = new Expr.Binary(
                new Expr.Grouping(
                        new Expr.Binary(
                                new Expr.Literal(1),
                                new Token(TokenType.PLUS, "+", null, 1),
                                new Expr.Literal(2))),
                new Token(TokenType.STAR, "*", null, 1),
                new Expr.Grouping(
                        new Expr.Binary(
                                new Expr.Literal(4),
                                new Token(TokenType.MINUS, "-", null, 1),
                                new Expr.Literal(3))));
        System.out.println(new Rpn().print(expression));
    }
}
