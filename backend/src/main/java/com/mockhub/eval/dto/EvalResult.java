package com.mockhub.eval.dto;

public record EvalResult(
        String conditionName,
        boolean passed,
        EvalSeverity severity,
        String message
) {

    public static EvalResult pass(String conditionName) {
        return new EvalResult(conditionName, true, EvalSeverity.INFO, "Passed");
    }

    public static EvalResult fail(String conditionName, EvalSeverity severity, String message) {
        return new EvalResult(conditionName, false, severity, message);
    }

    public static EvalResult skip(String conditionName, String reason) {
        return new EvalResult(conditionName, true, EvalSeverity.INFO, "Skipped: " + reason);
    }
}
