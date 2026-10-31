package com.example.analogalu;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class WaferProgramRuntime {
    private static final Set<String> PORTS = Set.of("front", "back", "left", "right");

    private WaferProgramRuntime() {}

    public record ExecutionResult(Map<String, Integer> ports, List<String> output,
                                  long elapsedMillis, String error) {
        public boolean successful() {
            return error == null;
        }
    }

    private record PairValue(int value, boolean flag, boolean borrow) {}
    private record MemoryReference(String name) {}
    private record Memory(byte[] data, int length, boolean bits) {
        private int read(int index) {
            return bits ? ((data[index >>> 3] >>> (index & 7)) & 1) : (data[index] & 15);
        }

        private void write(int index, int value) {
            if (bits) {
                int mask = 1 << (index & 7);
                if ((value & 1) == 0) data[index >>> 3] &= (byte) ~mask;
                else data[index >>> 3] |= (byte) mask;
            } else {
                data[index] = (byte) (value & 15);
            }
        }
    }

    public static ExecutionResult execute(String bytecode, Map<String, Integer> initialPorts) {
        Map<String, Object> variables = new HashMap<>();
        Map<String, Integer> ports = new HashMap<>();
        Map<String, Memory> memories = new HashMap<>();
        List<String> output = new ArrayList<>();
        ArrayDeque<Object> stack = new ArrayDeque<>();
        long elapsedMillis = 0;
        long allocatedBytes = 0;

        for (String port : PORTS) {
            int value = initialPorts == null ? 0 : initialPorts.getOrDefault(port, 0);
            ports.put(port, value & 15);
            variables.put(port, value & 15);
        }

        try {
            String image = decode(bytecode);
            String[] lines = image.split("\\R");
            if (lines.length == 0 || !lines[0].equals("WBC1")) {
                throw new IllegalArgumentException("Unsupported bytecode version");
            }
            for (int line = 1; line < lines.length; line++) {
                if (lines[line].isEmpty()) continue;
                String[] parts = lines[line].split("\\t", -1);
                String opcode = parts[0];
                switch (opcode) {
                    case "END" -> elapsedMillis++;
                    case "PUSH" -> stack.push(Integer.parseInt(parts[1]));
                    case "LOAD" -> {
                        if (!variables.containsKey(parts[1])) throw new IllegalStateException("Uninitialized variable " + parts[1]);
                        stack.push(variables.get(parts[1]));
                    }
                    case "MEM" -> {
                        if (!memories.containsKey(parts[1])) throw new IllegalStateException("Unknown memory " + parts[1]);
                        stack.push(new MemoryReference(parts[1]));
                    }
                    case "ALLOC_CRAM", "ALLOC_FF" -> {
                        boolean bits = opcode.equals("ALLOC_FF");
                        int size = Integer.parseInt(parts[2]);
                        long bytes = bits ? (size + 7L) / 8 : size;
                        if (size < 1 || bytes + allocatedBytes > WaferProgramCompiler.MAX_MEMORY_BYTES) {
                            throw new IllegalStateException("CRAM and FF storage must not exceed 512 bytes");
                        }
                        if (memories.putIfAbsent(parts[1], allocate(size, bits)) != null) {
                            throw new IllegalStateException("Memory already allocated: " + parts[1]);
                        }
                        allocatedBytes += bytes;
                    }
                    case "STORE" -> {
                        Object value = pop(stack);
                        if (PORTS.contains(parts[1])) {
                            int portValue = asInt(value) & 15;
                            ports.put(parts[1], portValue);
                            variables.put(parts[1], portValue);
                        } else {
                            variables.put(parts[1], value);
                        }
                    }
                    case "STORE_INDEX" -> {
                        Object value = pop(stack);
                        int index = asInt(pop(stack));
                        Memory memory = requireMemory(memories, parts[1]);
                        checkIndex(index, memory.length());
                        int stored = asInt(value) & 15;
                        memory.write(index, stored);
                    }
                    case "INDEX" -> {
                        int index = asInt(pop(stack));
                        Object reference = pop(stack);
                        if (!(reference instanceof MemoryReference memoryReference)) {
                            throw new IllegalStateException("Indexing requires CRAM or FF storage");
                        }
                        Memory memory = requireMemory(memories, memoryReference.name());
                        checkIndex(index, memory.length());
                        stack.push(memory.read(index));
                    }
                    case "SHOW" -> output.add(format(pop(stack)));
                    case "CARRY" -> {
                        Object value = pop(stack);
                        if (!(value instanceof PairValue pair) || pair.borrow()) {
                            throw new IllegalStateException(".carry requires a ++ result");
                        }
                        stack.push(pair.flag() ? 1 : 0);
                    }
                    case "BORROW" -> {
                        Object value = pop(stack);
                        if (!(value instanceof PairValue pair) || !pair.borrow()) {
                            throw new IllegalStateException(".borrow requires a -- result");
                        }
                        stack.push(pair.flag() ? 1 : 0);
                    }
                    case "POS", "NEG", "LOGICAL_NOT", "BITWISE_NOT" -> {
                        int value = asInt(pop(stack));
                        stack.push(switch (opcode) {
                            case "POS" -> value & 15;
                            case "NEG" -> -value & 15;
                            case "LOGICAL_NOT" -> value == 0 ? 15 : 0;
                            default -> ~value & 15;
                        });
                    }
                    default -> applyBinary(opcode, stack);
                }
            }
            return new ExecutionResult(Map.copyOf(ports), List.copyOf(output), elapsedMillis, null);
        } catch (RuntimeException exception) {
            return new ExecutionResult(Map.copyOf(ports), List.copyOf(output), elapsedMillis,
                    exception.getMessage() == null ? "Invalid bytecode" : exception.getMessage());
        }
    }

    private static String decode(String bytecode) {
        if (!WaferProgramCompiler.isBytecode(bytecode)) throw new IllegalArgumentException("Missing compiled wafer program");
        byte[] bytes = Base64.getDecoder().decode(bytecode.substring(WaferProgramCompiler.BYTECODE_PREFIX.length()));
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static Memory allocate(int size, boolean bits) {
        int storageBytes = bits ? (size + 7) / 8 : size;
        return new Memory(new byte[storageBytes], size, bits);
    }

    private static Memory requireMemory(Map<String, Memory> memories, String name) {
        Memory memory = memories.get(name);
        if (memory == null) throw new IllegalStateException("Unknown memory " + name);
        return memory;
    }

    private static void checkIndex(int index, int size) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException("Memory index " + index + " is out of bounds");
    }

    private static Object pop(ArrayDeque<Object> stack) {
        if (stack.isEmpty()) throw new IllegalStateException("Bytecode stack underflow");
        return stack.pop();
    }

    private static int asInt(Object value) {
        if (value instanceof Integer number) return number;
        throw new IllegalStateException("A carry/borrow pair cannot be used as an integer");
    }

    private static String format(Object value) {
        if (value instanceof PairValue pair) {
            return "(" + pair.value() + ", " + (pair.flag() ? 1 : 0) + ")";
        }
        return Integer.toString(asInt(value));
    }

    private static void applyBinary(String opcode, ArrayDeque<Object> stack) {
        int right = asInt(pop(stack));
        int left = asInt(pop(stack));
        int shift = right & 31;
        switch (opcode) {
            case "ADD" -> stack.push((left + right) & 15);
            case "ADD_CARRY" -> {
                int sum = left + right;
                stack.push(new PairValue(sum & 15, sum > 15, false));
            }
            case "SUB" -> stack.push((left - right) & 15);
            case "SUB_BORROW" -> stack.push(new PairValue((left - right) & 15, left < right, true));
            case "MUL" -> stack.push((left * right) & 15);
            case "DIV" -> stack.push((left / right) & 15);
            case "MOD" -> stack.push((left % right) & 15);
            case "SHL" -> stack.push(shift >= 4 ? 0 : (left << shift) & 15);
            case "SHR" -> stack.push(shift >= 4 ? 0 : left >>> shift);
            case "ROL" -> {
                int amount = right & 3;
                stack.push(amount == 0 ? left & 15 : ((left << amount) | (left >>> (4 - amount))) & 15);
            }
            case "ROR" -> {
                int amount = right & 3;
                stack.push(amount == 0 ? left & 15 : ((left >>> amount) | (left << (4 - amount))) & 15);
            }
            case "AND" -> stack.push(left & right & 15);
            case "OR" -> stack.push((left | right) & 15);
            case "XOR" -> stack.push((left ^ right) & 15);
            default -> throw new IllegalArgumentException("Unknown bytecode operation " + opcode);
        }
    }
}
