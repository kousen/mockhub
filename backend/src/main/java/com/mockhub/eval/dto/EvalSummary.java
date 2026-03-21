package com.mockhub.eval.dto;

import java.util.List;

public record EvalSummary(
        List<EvalResult> results
) {

    public boolean allPassed() {
        return results.stream().allMatch(EvalResult::passed);
    }

    public List<EvalResult> failures() {
        return results.stream()
                .filter(r -> !r.passed())
                .toList();
    }

    public boolean hasCriticalFailure() {
        return results.stream()
                .anyMatch(r -> !r.passed() && r.severity() == EvalSeverity.CRITICAL);
    }
}
