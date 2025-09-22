package me.elinge.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

public class Lox {
    private static final Interpreter interpreter = new Interpreter();
    static boolean hadError = false;
    static boolean hadRuntimeError = false;

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            runPrompt();
        } else {
            for (var file : args) {
                runFile(file);
            }
        }
    }

    private static void runFile(String path) throws IOException {
        var bytes = Files.readAllBytes(Path.of(path));
        run(new String(bytes, Charset.defaultCharset()));
        if (hadError) {
            System.exit(65); // EX_DATAERR
        }
        if (hadRuntimeError) {
            System.exit(70); // EX_SOFTWARE
        }
    }

    private static void runPrompt() throws IOException {
        var input = new InputStreamReader(System.in);
        var reader = new BufferedReader(input);

        while (true) {
            System.out.print("> ");
            var line = reader.readLine();
            if (line == null) {
                break;
            }
            run(line);
            hadError = false;
            hadRuntimeError = false;
        }
    }

    private static void run(String source) {
        var scanner = new Scanner(source);
        var tokens = scanner.scanTokens();

        var parser = new Parser(tokens);
        var statements = parser.parse();

        // Stop if there was a syntax error.
        if (hadError) {
            return;
        }

        interpreter.interpret(statements);
    }

    static void error(int line, String message) {
        report(line, "", message);
    }

    private static void report(int line, String where, String message) {
        System.err.printf("[line %d] Error%s: %s%n", line, where, message);
        hadError = true;
    }

    static void error(Token token, String message) {
        if (token.type() == TokenType.EOF) {
            report(token.line(), " at the end", message);
        } else {
            report(token.line(), " at '" + token.lexeme() + "'", message);
        }
    }

    static void runtimeError(RuntimeError error) {
        System.err.printf("%s\n[line %d]%n", error.getMessage(), error.token.line());
        hadRuntimeError = true;
    }
}
