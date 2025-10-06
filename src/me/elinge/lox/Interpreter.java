package me.elinge.lox;

import java.util.ArrayList;
import java.util.List;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    final Environment globals = new Environment();
    private Environment environment = globals;

    Interpreter() {
        globals.define("clock", new Callable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return (double)System.currentTimeMillis() / 1000.0;
            }

            @Override
            public String toString() {
                return "<native fn>";
            }
        });
    }

    void interpret(List<Stmt> statements) {
        try {
            for (var statement : statements) {
                // Execute statement.
                statement.accept(this);
            }
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        var left = expr.left.accept(this);
        var right = expr.right.accept(this);

        return switch (expr.operator.type()) {
            case GREATER -> {
                if (left instanceof Double && right instanceof Double) {
                    yield (double)left > (double)right;
                }

                if (left instanceof String a && right instanceof String b) {
                    for (int i = 0; i < a.length() && i < b.length(); i++) {
                        if (a.codePointAt(i) > b.codePointAt(i)) {
                            yield true;
                        }
                    }

                    yield false;
                }

                throw new RuntimeError(expr.operator, "Operands must be both numbers or both strings.");
            }
            case GREATER_EQUAL -> {
                if (left instanceof Double && right instanceof Double) {
                    yield (double)left >= (double)right;
                }

                if (left instanceof String a && right instanceof String b) {
                    for (int i = 0; i < a.length() && i < b.length(); i++) {
                        if (a.codePointAt(i) >= b.codePointAt(i)) {
                            yield true;
                        }
                    }

                    yield false;
                }

                throw new RuntimeError(expr.operator, "Operands must be both numbers or both strings.");
            }
            case LESS -> {
                if (left instanceof Double && right instanceof Double) {
                    yield (double)left < (double)right;
                }

                if (left instanceof String a && right instanceof String b) {
                    for (int i = 0; i < a.length() && i < b.length(); i++) {
                        if (a.codePointAt(i) < b.codePointAt(i)) {
                            yield true;
                        }
                    }

                    yield false;
                }

                throw new RuntimeError(expr.operator, "Operands must be both numbers or both strings.");
            }
            case LESS_EQUAL -> {
                if (left instanceof Double && right instanceof Double) {
                    yield (double)left <= (double)right;
                }

                if (left instanceof String a && right instanceof String b) {
                    for (int i = 0; i < a.length() && i < b.length(); i++) {
                        if (a.codePointAt(i) <= b.codePointAt(i)) {
                            yield true;
                        }
                    }

                    yield false;
                }

                throw new RuntimeError(expr.operator, "Operands must be both numbers or both strings.");
            }
            case MINUS -> {
                checkNumberOperands(expr.operator, left, right);
                yield (double)left - (double)right;
            }
            case SLASH -> {
                checkNumberOperands(expr.operator, left, right);

                if ((double)right == 0.0) {
                    throw new RuntimeError(expr.operator, "Division by zero.");
                }

                yield (double)left / (double)right;
            }
            case STAR -> {
                checkNumberOperands(expr.operator, left, right);
                yield (double)left * (double)right;
            }
            case PLUS -> {
                if (left instanceof Double && right instanceof Double) {
                    yield (double)left + (double)right;
                }

                if (left == null) {
                    throw new RuntimeError(expr.operator, "Left-side operand is nil.");
                }

                if (right == null) {
                    throw new RuntimeError(expr.operator, "Right-side operand is nil.");
                }

                yield left.toString() + right;
            }
            case BANG_EQUAL -> !isEqual(left, right);
            case EQUAL_EQUAL -> isEqual(left, right);
            default -> throw new RuntimeException("Unhandled binary expression.");
        };
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        var callee = expr.callee.accept(this);

        var arguments = new ArrayList<>();
        for (var argument : expr.arguments) {
            arguments.add(argument.accept(this));
        }

        if (!(callee instanceof Callable function)) {
            throw new RuntimeError(expr.paren, "Can only call functions and classes.");
        }

        if (arguments.size() != function.arity()) {
            throw new RuntimeError(
                    expr.paren,
                    String.format("Expected %d arguments but got %d.", function.arity(), arguments.size()));
        }

        return function.call(this, arguments);
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return expr.expression.accept(this);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        var left = expr.left.accept(this);

        if (expr.operator.type() == TokenType.OR) {
            if (isTruthy(left)) {
                return left;
            }
        } else {
            if (!isTruthy(left)) {
                return left;
            }
        }

        return expr.right.accept(this);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        var right = expr.right.accept(this);

        return switch (expr.operator.type()) {
            case BANG -> !isTruthy(right);
            case MINUS -> {
                checkNumberOperand(expr.operator, right);
                yield -(double)right;
            }
            default -> throw new RuntimeException("Unhandled unary expression.");
        };
    }

    @Override
    public Object visitTernaryExpr(Expr.Ternary expr) {
        return isTruthy(expr.left.accept(this)) ? expr.middle.accept(this) : expr.right.accept(this);
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.get(expr.name);
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (!(operand instanceof Double)) {
            throw new RuntimeError(operator, "Operand must be a number.");
        }
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (!(left instanceof Double && right instanceof Double)) {
            throw new RuntimeError(operator, "Operands must be numbers.");
        }
    }

    private boolean isTruthy(Object object) {
        if (object == null) {
            return false;
        }

        if (object instanceof Boolean) {
            return (boolean)object;
        }

        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) {
            return true;
        }

        if (a == null) {
            return false;
        }

        // Beware: this makes NaN equal to NaN! NaN should not be equal to anything else per IEEE 754.
        return a.equals(b);
    }

    private String stringify(Object object) {
        if (object == null) {
            return "nil";
        }

        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        return object.toString();
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        stmt.expression.accept(this);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        var function = new Function(stmt, environment);
        environment.define(stmt.name.lexeme(), function);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(stmt.condition.accept(this))) {
            stmt.thenBranch.accept(this);
        } else if (stmt.elseBranch != null) {
            stmt.elseBranch.accept(this);
        }

        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        System.out.println(stringify(stmt.expression.accept(this)));
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        var value = stmt.value == null ? null : stmt.value.accept(this);
        throw new Return(value);
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        var value = stmt.initializer == null ? null : stmt.initializer.accept(this);
        environment.define(stmt.name.lexeme(), value);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while (isTruthy(stmt.condition.accept(this))) {
            stmt.body.accept(this);
        }

        return null;
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        var value = expr.value.accept(this);
        environment.assign(expr.name, value);
        return value;
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    void executeBlock(List<Stmt> statements, Environment environment) {
        var previous = this.environment;
        try {
            this.environment = environment;

            for (var statement : statements) {
                statement.accept(this);
            }
        } finally {
            this.environment = previous;
        }
    }
}
