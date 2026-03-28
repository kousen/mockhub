package com.mockhub.mcp.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ToolTimingAspect {

    private static final Logger log = LoggerFactory.getLogger(ToolTimingAspect.class);

    @Around("@annotation(org.springframework.ai.tool.annotation.Tool)")
    public Object timeToolExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String toolName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        long start = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - start;
            log.info("MCP tool {}.{} completed in {}ms", className, toolName, elapsed);
            return result;
        } catch (Throwable e) {
            long elapsed = System.currentTimeMillis() - start;
            log.warn("MCP tool {}.{} failed in {}ms: {}", className, toolName, elapsed, e.getMessage());
            throw e;
        }
    }
}
