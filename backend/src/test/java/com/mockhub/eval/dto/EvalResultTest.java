package com.mockhub.eval.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EvalResult")
class EvalResultTest {

    @Test
    @DisplayName("pass() creates a passing result with INFO severity")
    void pass_givenConditionName_createsPassingResult() {
        EvalResult result = EvalResult.pass("test-condition");

        assertTrue(result.passed());
        assertEquals("test-condition", result.conditionName());
        assertEquals(EvalSeverity.INFO, result.severity());
        assertEquals("Passed", result.message());
    }

    @Test
    @DisplayName("fail() creates a failing result with specified severity")
    void fail_givenSeverityAndMessage_createsFailingResult() {
        EvalResult result = EvalResult.fail("price-check", EvalSeverity.CRITICAL, "Price out of range");

        assertFalse(result.passed());
        assertEquals("price-check", result.conditionName());
        assertEquals(EvalSeverity.CRITICAL, result.severity());
        assertEquals("Price out of range", result.message());
    }

    @Test
    @DisplayName("fail() with WARNING severity")
    void fail_givenWarningSeverity_createsWarningResult() {
        EvalResult result = EvalResult.fail("availability", EvalSeverity.WARNING, "Event has low availability");

        assertFalse(result.passed());
        assertEquals(EvalSeverity.WARNING, result.severity());
    }

    @Test
    @DisplayName("skip() creates a passing result with skip reason")
    void skip_givenReason_createsSkippedResult() {
        EvalResult result = EvalResult.skip("grounding", "AI judge disabled");

        assertTrue(result.passed());
        assertEquals("grounding", result.conditionName());
        assertEquals(EvalSeverity.INFO, result.severity());
        assertEquals("Skipped: AI judge disabled", result.message());
    }
}
