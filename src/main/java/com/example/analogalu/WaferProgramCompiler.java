package com.example.analogalu;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class WaferProgramCompiler {
    public static final String BYTECODE_PREFIX = "WBC1:";
    public static final long MAX_MEMORY_BYTES = 512;
    private static final Set<String> PORTS = Set.of("front", "back", "left", "right");
    private static final Set<String> BINARY_OPERATORS = Set.of(
            "+", "++", "-", "--", "*", "/", "%", "<<", ">>", "<<<", ">>>", "&", "|", "^");

    private WaferProgramCompiler() {}

    public record Compilation(String bytecode, int statementCount, long cramBytes, long ffBits, String error) {
        public boolean successful() {
            return error == null;
        }
    }

    private record Token(String text, int line) {}

    private static final class CompileException extends RuntimeException {
        private CompileException(int line, String message) {
            super("Line " + line + ": " + message);
        }
    }

    public static Compilation compile(String source) {
        if (source == null) return new Compilation("", 0, 0, 0, "Program cannot be empty");
        try {
            return new Parser(source).compile();
        } catch (CompileException exception) {
            return new Compilation("", 0, 0, 0, exception.getMessage());
        }
    }

    public static boolean isBytecode(String value) {
        return value != null && value.startsWith(BYTECODE_PREFIX);
    }

    private static final class Parser {
        private final List<Token> tokens;
        private final List<String> instructions = new ArrayList<>();
        private final Set<String> symbols = new HashSet<>();
        private final Map<String, Boolean> memories = new HashMap<>();
        private final Set<String> declaredPorts = new HashSet<>();
        private int cursor;
        private int statementCount;
        private long cramBytes;
        private long ffBits;
        private long ffBytes;
        private boolean moduleDeclared;
        private boolean executableStarted;

        private Parser(String source) {
            tokens = tokenize(source);
        }

        private Compilation compile() {
            while (!atEnd()) parseStatement();

            if (!moduleDeclared) throw errorAtEnd("Declare a module first: module main;");
            if (!declaredPorts.equals(PORTS)) {
                throw errorAtEnd("Declare each 4-bit port: front, back, left, and right");
            }
            if (cramBytes + ffBytes > MAX_MEMORY_BYTES) {
                throw errorAtEnd("CRAM and FF storage must not exceed 512 bytes per semiconductor");
            }

            StringBuilder image = new StringBuilder("WBC1\n");
            for (String instruction : instructions) image.append(instruction).append('\n');
            String encoded = BYTECODE_PREFIX + Base64.getEncoder().encodeToString(
                    image.toString().getBytes(StandardCharsets.UTF_8));
            return new Compilation(encoded, statementCount, cramBytes, ffBits, null);
        }

        private void parseStatement() {
            Token start = peek();
            switch (start.text()) {
                case "module" -> parseModule();
                case "port" -> parsePort();
                case "cram" -> parseMemory(false);
                case "ff" -> parseMemory(true);
                case "let" -> parseLet();
                case "show" -> parseShow();
                default -> {
                    if (isIdentifier(start.text()) && isAssignmentAhead()) parseAssignment();
                    else throw error(start, "Expected a declaration, assignment, or show(expression);");
                }
            }
            emit("END");
            statementCount++;
        }

        private void parseModule() {
            Token keyword = take();
            if (moduleDeclared || statementCount != 0) {
                throw error(keyword, "Module declaration must be first and appear once");
            }
            expectIdentifier("module name");
            expect(";");
            moduleDeclared = true;
        }

        private void parsePort() {
            Token keyword = take();
            ensureDeclarationSection(keyword);
            Token name = expectIdentifier("port name");
            if (!PORTS.contains(name.text())) throw error(name, "Unknown port " + name.text());
            if (!declaredPorts.add(name.text())) throw error(name, "Port declared more than once: " + name.text());
            expect(":");
            Token width = take();
            if (!width.text().equals("4")) throw error(width, "Ports must be 4-bit");
            expect(";");
            symbols.add(name.text());
        }

        private void parseMemory(boolean flipFlop) {
            Token keyword = take();
            ensureDeclarationSection(keyword);
            Token name = expectIdentifier(flipFlop ? "FF name" : "CRAM name");
            declareSymbol(name);
            expect(":");
            Token size = take();
            long amount;
            try {
                amount = Long.parseLong(size.text());
            } catch (NumberFormatException exception) {
                throw error(size, "Storage size must be a positive integer");
            }
            long maximum = flipFlop ? MAX_MEMORY_BYTES * 8 : MAX_MEMORY_BYTES;
            if (amount < 1 || amount > maximum) {
                throw error(size, "Storage size exceeds the 512-byte semiconductor limit");
            }
            expect(";");
            if (flipFlop) {
                ffBits += amount;
                ffBytes += (amount + 7) / 8;
            }
            else cramBytes += amount;
            memories.put(name.text(), flipFlop);
            if (cramBytes + ffBytes > MAX_MEMORY_BYTES) {
                throw error(keyword, "CRAM and FF storage must not exceed 512 bytes per semiconductor");
            }
            emit((flipFlop ? "ALLOC_FF\t" : "ALLOC_CRAM\t") + name.text() + "\t" + amount);
        }

        private void parseLet() {
            take();
            executableStarted = true;
            Token name = expectIdentifier("variable name");
            if (PORTS.contains(name.text()) || name.text().equals("show")) {
                throw error(name, "Reserved name cannot be declared: " + name.text());
            }
            if (symbols.contains(name.text())) throw error(name, "Variable already declared: " + name.text());
            expect("=");
            parseExpression(0);
            expect(";");
            symbols.add(name.text());
            emit("STORE\t" + name.text());
        }

        private void parseAssignment() {
            Token name = take();
            executableStarted = true;
            if (!symbols.contains(name.text())) throw error(name, "Unknown name " + name.text());
            if (match("[")) {
                if (!memories.containsKey(name.text())) throw error(name, "Only CRAM/FF variables can be indexed");
                parseExpression(0);
                expect("]");
                expect("=");
                parseExpression(0);
                expect(";");
                emit("STORE_INDEX\t" + name.text());
                return;
            }
            if (memories.containsKey(name.text())) throw error(name, "Memory assignment requires an index");
            expect("=");
            parseExpression(0);
            expect(";");
            emit("STORE\t" + name.text());
        }

        private void parseShow() {
            Token keyword = take();
            executableStarted = true;
            expect("(");
            parseExpression(0);
            expect(")");
            expect(";");
            emit("SHOW");
        }

        private void ensureDeclarationSection(Token token) {
            if (!moduleDeclared) throw error(token, "Declare a module before ports and storage");
            if (executableStarted) {
                throw error(token, "Ports and storage must be declared before executable statements");
            }
        }

        private void parseExpression(int minimumPrecedence) {
            parsePrefix();
            while (!atEnd()) {
                if (match(".")) {
                    Token property = expectIdentifier("property name");
                    if (property.text().equals("carry")) emit("CARRY");
                    else if (property.text().equals("borrow")) emit("BORROW");
                    else throw error(property, "Only .carry and .borrow properties are supported");
                    continue;
                }
                if (match("[")) {
                    parseExpression(0);
                    expect("]");
                    emit("INDEX");
                    continue;
                }
                String operator = peek().text();
                int precedence = precedence(operator);
                if (precedence < minimumPrecedence) break;
                take();
                parseExpression(precedence + 1);
                emit(binaryOpcode(operator));
            }
        }

        private void parsePrefix() {
            Token token = take();
            if (token.text().matches("[0-9]+")) {
                int value;
                try {
                    value = Integer.parseInt(token.text());
                } catch (NumberFormatException exception) {
                    throw error(token, "Integer literal is too large");
                }
                if (value > 15) throw error(token, "Integer literals must be in the range 0..15");
                emit("PUSH\t" + value);
                return;
            }
            if (token.text().equals("(")) {
                parseExpression(0);
                expect(")");
                return;
            }
            if (isIdentifier(token.text())) {
                if (!symbols.contains(token.text())) throw error(token, "Unknown name " + token.text());
                emit(memories.containsKey(token.text()) ? "MEM\t" + token.text() : "LOAD\t" + token.text());
                return;
            }
            if (token.text().equals("+") || token.text().equals("-")
                    || token.text().equals("!") || token.text().equals("~")) {
                if (match("(")) {
                    parseExpression(0);
                    if (match(",")) {
                        parseExpression(0);
                        expect(")");
                        emit(binaryOpcode(token.text()));
                    } else {
                        expect(")");
                        emit(unaryOpcode(token.text()));
                    }
                } else {
                    parseExpression(7);
                    emit(unaryOpcode(token.text()));
                }
                return;
            }
            if (BINARY_OPERATORS.contains(token.text())) {
                expect("(");
                parseExpression(0);
                expect(",");
                parseExpression(0);
                expect(")");
                emit(binaryOpcode(token.text()));
                return;
            }
            throw error(token, "Expected a value or operator");
        }

        private boolean isAssignmentAhead() {
            return lookAhead(1).text().equals("=") || lookAhead(1).text().equals("[");
        }

        private int precedence(String operator) {
            return switch (operator) {
                case "|" -> 1;
                case "^" -> 2;
                case "&" -> 3;
                case "<<", ">>", "<<<", ">>>" -> 4;
                case "+", "++", "-", "--" -> 5;
                case "*", "/", "%" -> 6;
                default -> -1;
            };
        }

        private String binaryOpcode(String operator) {
            return switch (operator) {
                case "+" -> "ADD";
                case "++" -> "ADD_CARRY";
                case "-" -> "SUB";
                case "--" -> "SUB_BORROW";
                case "*" -> "MUL";
                case "/" -> "DIV";
                case "%" -> "MOD";
                case "<<" -> "SHL";
                case ">>" -> "SHR";
                case "<<<" -> "ROL";
                case ">>>" -> "ROR";
                case "&" -> "AND";
                case "|" -> "OR";
                case "^" -> "XOR";
                default -> throw error(peek(), "Unsupported operator " + operator);
            };
        }

        private String unaryOpcode(String operator) {
            return switch (operator) {
                case "+" -> "POS";
                case "-" -> "NEG";
                case "!" -> "LOGICAL_NOT";
                case "~" -> "BITWISE_NOT";
                default -> throw error(peek(), "Unsupported unary operator " + operator);
            };
        }

        private void declareSymbol(Token name) {
            if (PORTS.contains(name.text()) || name.text().equals("show")) {
                throw error(name, "Reserved name cannot be declared: " + name.text());
            }
            if (!symbols.add(name.text())) throw error(name, "Variable already declared: " + name.text());
        }

        private Token expectIdentifier(String description) {
            Token token = take();
            if (!isIdentifier(token.text())) throw error(token, "Expected " + description);
            return token;
        }

        private void expect(String text) {
            Token token = take();
            if (!token.text().equals(text)) throw error(token, "Expected '" + text + "'");
        }

        private boolean match(String text) {
            if (atEnd() || !peek().text().equals(text)) return false;
            cursor++;
            return true;
        }

        private void emit(String instruction) {
            instructions.add(instruction);
        }

        private Token peek() {
            return tokens.get(cursor);
        }

        private Token lookAhead(int offset) {
            int position = cursor + offset;
            return position < tokens.size() ? tokens.get(position) : new Token("<end>", lastLine());
        }

        private Token take() {
            if (atEnd()) throw errorAtEnd("Unexpected end of program");
            return tokens.get(cursor++);
        }

        private boolean atEnd() {
            return cursor >= tokens.size();
        }

        private int lastLine() {
            return tokens.isEmpty() ? 1 : tokens.get(tokens.size() - 1).line();
        }

        private CompileException error(Token token, String message) {
            return new CompileException(token.line(), message);
        }

        private CompileException errorAtEnd(String message) {
            return new CompileException(lastLine(), message);
        }

        private static boolean isIdentifier(String text) {
            return text.matches("[A-Za-z_][A-Za-z_0-9]*");
        }

        private static List<Token> tokenize(String source) {
            List<Token> result = new ArrayList<>();
            String[] lines = source.split("\\R", -1);
            String[] operators = { ">>>", "<<<", "++", "--", "<<", ">>" };
            for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
                String line = lines[lineIndex];
                int comment = line.indexOf("//");
                if (comment >= 0) line = line.substring(0, comment);
                for (int index = 0; index < line.length();) {
                    char current = line.charAt(index);
                    if (Character.isWhitespace(current)) {
                        index++;
                        continue;
                    }
                    int start = index;
                    if (Character.isDigit(current)) {
                        while (index < line.length() && Character.isDigit(line.charAt(index))) index++;
                    } else if (Character.isLetter(current) || current == '_') {
                        while (index < line.length()
                                && (Character.isLetterOrDigit(line.charAt(index)) || line.charAt(index) == '_')) index++;
                    } else {
                        String operator = null;
                        for (String candidate : operators) {
                            if (line.startsWith(candidate, index)) {
                                operator = candidate;
                                break;
                            }
                        }
                        if (operator != null) index += operator.length();
                        else if ("+-*/%&|^~!()[],:;.=".indexOf(current) >= 0) index++;
                        else throw new CompileException(lineIndex + 1, "Unsupported character '" + current + "'");
                    }
                    result.add(new Token(line.substring(start, index), lineIndex + 1));
                }
            }
            return result;
        }
    }
}