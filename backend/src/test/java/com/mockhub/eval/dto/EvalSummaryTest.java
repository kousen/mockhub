package com.mockhub.eval.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EvalSummary")
class EvalSummaryTest {

    @Test
    @DisplayName("allPassed returns true when all results pass")
    void allPassed_givenAllPassing_returnsTrue() {
        EvalSummary summary = new EvalSummary(List.of(
                EvalResult.pass("condition-1"),
                EvalResult.pass("condition-2"),
                EvalResult.skip("condition-3", "disabled")
        ));

        assertTrue(summary.allPassed());
    }

    @Test
    @DisplayName("allPassed returns false when any result fails")
    void allPassed_givenOneFailure_returnsFalse() {
        EvalSummary summary = new EvalSummary(List.of(
                EvalResult.pass("condition-1"),
                EvalResult.fail("condition-2", EvalSeverity.WARNING, "problem")
        ));

        assertFalse(summary.allPassed());
    }

    @Test
    @DisplayName("failures returns only failing results")
    void failures_givenMixedResults_returnsOnlyFailures() {
        EvalResult failure = EvalResult.fail("bad-one", EvalSeverity.CRITICAL, "broken");
        EvalSummary summary = new EvalSummary(List.of(
                EvalResult.pass("good-one"),
                failure,
                EvalResult.pass("good-two")
        ));

        List<EvalResult> failures = summary.failures();
        assertEquals(1, failures.size());
        assertEquals("bad-one", failures.get(0).conditionName());
    }

    @Test
    @DisplayName("failures returns empty list when all pass")
    void failures_givenAllPassing_returnsEmptyList() {
        EvalSummary summary = new EvalSummary(List.of(
                EvalResult.pass("condition-1")
        ));

        assertTrue(summary.failures().isEmpty());
    }

    @Test
    @DisplayName("hasCriticalFailure returns true when CRITICAL failure exists")
    void hasCriticalFailure_givenCriticalFailure_returnsTrue() {
        EvalSummary summary = new EvalSummary(List.of(
                EvalResult.pass("ok"),
                EvalResult.fail("critical-one", EvalSeverity.CRITICAL, "bad")
        ));

        assertTrue(summary.hasCriticalFailure());
    }

    @Test
    @DisplayName("hasCriticalFailure returns false when only WARNING failures exist")
    void hasCriticalFailure_givenOnlyWarnings_returnsFalse() {
        EvalSummary summary = new EvalSummary(List.of(
                EvalResult.fail("warn-one", EvalSeverity.WARNING, "not great"),
                EvalResult.pass("ok")
        ));

        assertFalse(summary.hasCriticalFailure());
    }

    @Test
    @DisplayName("hasCriticalFailure returns false when all pass")
    void hasCriticalFailure_givenAllPassing_returnsFalse() {
        EvalSummary summary = new EvalSummary(List.of(
                EvalResult.pass("ok")
        ));

        assertFalse(summary.hasCriticalFailure());
    }

    @Test
    @DisplayName("empty results list means all passed")
    void allPassed_givenEmptyResults_returnsTrue() {
        EvalSummary summary = new EvalSummary(List.of());

        assertTrue(summary.allPassed());
        assertTrue(summary.failures().isEmpty());
        assertFalse(summary.hasCriticalFailure());
    }
}
