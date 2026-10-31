package com.example.analogalu;

import java.util.Optional;
import java.util.OptionalLong;

public final class WaferProgramValidator {
    private WaferProgramValidator() {}

    public static Optional<String> validate(String code) {
        WaferProgramCompiler.Compilation compilation = WaferProgramCompiler.compile(code);
        return compilation.successful() ? Optional.empty() : Optional.of(compilation.error());
    }

    public static OptionalLong estimateDurationMillis(String code) {
        WaferProgramCompiler.Compilation compilation = WaferProgramCompiler.compile(code);
        return compilation.successful()
                ? OptionalLong.of(compilation.statementCount())
                : OptionalLong.empty();
    }
}