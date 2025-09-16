package me.elinge.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

public class Lox {
    static boolean hadError = false;

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Usage: jlox [script]");
            System.exit(64); // EX_USAGE
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    private static void runFile(String path) throws IOException {
        var bytes = Files.readAllBytes(Path.of(path));
        run(new String(bytes, Charset.defaultCharset()));
        if (hadError) {
            System.exit(65); // EX_DATAERR
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
        }
    }

    private static void run(String source) {
        var scanner = new Scanner(source);
        var tokens = scanner.scanTokens();

        // For now, just print the tokens.
        for (var token : tokens) {
            System.out.println(token);
        }
    }

    static void error(int line, String message) {
        report(line, message);
    }

    private static void report(int line, String message) {
        System.err.printf("[line %d] Error: %s%n", line, message);
        hadError = true;
    }
}
