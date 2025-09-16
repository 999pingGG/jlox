package me.elinge.lox;

record Token(TokenType type, String lexeme, Object literal, int line) {
    public String toString() {
        return type + " " + lexeme + " " + literal;
    }
}
