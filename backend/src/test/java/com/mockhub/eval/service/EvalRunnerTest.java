package com.mockhub.eval.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mockhub.eval.EvalCondition;
import com.mockhub.eval.dto.EvalContext;
import com.mockhub.eval.dto.EvalResult;
import com.mockhub.eval.dto.EvalSeverity;
import com.mockhub.eval.dto.EvalSummary;

@ExtendWith(MockitoExtension.class)
@DisplayName("EvalRunner")
class EvalRunnerTest {

    @Mock
    private EvalCondition condition1;

    @Mock
    private EvalCondition condition2;

    @Mock
    private EvalCondition condition3;

    @Test
    @DisplayName("evaluate runs only conditions that apply to the context")
    void evaluate_givenMixedApplicability_runsOnlyApplicableConditions() {
        EvalContext context = EvalContext.forChat("response", "query");

        when(condition1.appliesTo(context)).thenReturn(true);
        when(condition1.evaluate(context)).thenReturn(EvalResult.pass("condition-1"));

        when(condition2.appliesTo(context)).thenReturn(false);

        EvalRunner runner = new EvalRunner(List.of(condition1, condition2));
        EvalSummary summary = runner.evaluate(context);

        assertEquals(1, summary.results().size());
        verify(condition2, never()).evaluate(any());
    }

    @Test
    @DisplayName("evaluate returns all results from applicable conditions")
    void evaluate_givenMultipleApplicable_returnsAllResults() {
        EvalContext context = EvalContext.forChat("response", "query");

        when(condition1.appliesTo(context)).thenReturn(true);
        when(condition1.evaluate(context)).thenReturn(EvalResult.pass("condition-1"));

        when(condition2.appliesTo(context)).thenReturn(true);
        when(condition2.evaluate(context)).thenReturn(
                EvalResult.fail("condition-2", EvalSeverity.WARNING, "problem"));

        EvalRunner runner = new EvalRunner(List.of(condition1, condition2));
        EvalSummary summary = runner.evaluate(context);

        assertEquals(2, summary.results().size());
        assertFalse(summary.allPassed());
        assertEquals(1, summary.failures().size());
    }

    @Test
    @DisplayName("evaluate returns empty summary when no conditions apply")
    void evaluate_givenNoApplicableConditions_returnsEmptySummary() {
        EvalContext context = EvalContext.forChat("response", "query");

        when(condition1.appliesTo(context)).thenReturn(false);

        EvalRunner runner = new EvalRunner(List.of(condition1));
        EvalSummary summary = runner.evaluate(context);

        assertTrue(summary.results().isEmpty());
        assertTrue(summary.allPassed());
    }

    @Test
    @DisplayName("evaluate handles empty conditions list")
    void evaluate_givenNoConditions_returnsEmptySummary() {
        EvalContext context = EvalContext.forChat("response", "query");

        EvalRunner runner = new EvalRunner(List.of());
        EvalSummary summary = runner.evaluate(context);

        assertTrue(summary.results().isEmpty());
        assertTrue(summary.allPassed());
    }

    @Test
    @DisplayName("evaluate detects critical failure in summary")
    void evaluate_givenCriticalFailure_summaryReflectsCritical() {
        EvalContext context = EvalContext.forChat("response", "query");

        when(condition1.appliesTo(context)).thenReturn(true);
        when(condition1.evaluate(context)).thenReturn(EvalResult.pass("ok"));

        when(condition2.appliesTo(context)).thenReturn(true);
        when(condition2.evaluate(context)).thenReturn(
                EvalResult.fail("bad", EvalSeverity.CRITICAL, "critical problem"));

        EvalRunner runner = new EvalRunner(List.of(condition1, condition2));
        EvalSummary summary = runner.evaluate(context);

        assertTrue(summary.hasCriticalFailure());
    }
}
