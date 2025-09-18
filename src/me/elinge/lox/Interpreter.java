package me.elinge.lox;

class Interpreter implements Expr.Visitor<Object> {
    void interpret(Expr expression) {
        try {
            Object value = expression.accept(this);
            System.out.println(stringify(value));
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

                if (left instanceof String && right instanceof String) {
                    var a = (String)left;
                    var b = (String)right;

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

                if (left instanceof String && right instanceof String) {
                    var a = (String)left;
                    var b = (String)right;

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

                if (left instanceof String && right instanceof String) {
                    var a = (String)left;
                    var b = (String)right;

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

                if (left instanceof String && right instanceof String) {
                    var a = (String)left;
                    var b = (String)right;

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

                yield left.toString() + right.toString();
            }
            case BANG_EQUAL -> !isEqual(left, right);
            case EQUAL_EQUAL -> isEqual(left, right);
            default -> throw new RuntimeException("Unhandled binary expression.");
        };
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
}
