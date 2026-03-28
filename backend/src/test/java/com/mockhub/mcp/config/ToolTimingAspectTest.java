package com.mockhub.mcp.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ToolTimingAspectTest {

    private ToolTimingAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new ToolTimingAspect();
    }

    @Test
    @DisplayName("timeToolExecution - given successful tool call - returns result and logs timing")
    void timeToolExecution_givenSuccessfulCall_returnsResult() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("findTickets");
        when(joinPoint.getTarget()).thenReturn(new Object() {});
        when(joinPoint.proceed()).thenReturn("result-json");

        Object result = aspect.timeToolExecution(joinPoint);

        assertEquals("result-json", result);
    }

    @Test
    @DisplayName("timeToolExecution - given failing tool call - rethrows exception and logs timing")
    void timeToolExecution_givenFailingCall_rethrowsException() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("addToCart");
        when(joinPoint.getTarget()).thenReturn(new Object() {});
        when(joinPoint.proceed()).thenThrow(new RuntimeException("DB connection failed"));

        assertThrows(RuntimeException.class, () -> aspect.timeToolExecution(joinPoint));
    }
}
