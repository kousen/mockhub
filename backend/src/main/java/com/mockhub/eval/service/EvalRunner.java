package com.mockhub.eval.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.mockhub.eval.EvalCondition;
import com.mockhub.eval.dto.EvalContext;
import com.mockhub.eval.dto.EvalResult;
import com.mockhub.eval.dto.EvalSummary;

@Service
public class EvalRunner {

    private static final Logger log = LoggerFactory.getLogger(EvalRunner.class);

    private final List<EvalCondition> conditions;

    public EvalRunner(List<EvalCondition> conditions) {
        this.conditions = conditions;
    }

    public EvalSummary evaluate(EvalContext context) {
        List<EvalResult> results = conditions.stream()
                .filter(condition -> condition.appliesTo(context))
                .map(condition -> {
                    EvalResult result = condition.evaluate(context);
                    logResult(result);
                    return result;
                })
                .toList();
        return new EvalSummary(results);
    }

    private void logResult(EvalResult result) {
        if (!result.passed()) {
            log.warn("Eval FAILED [{}]: {} (severity={})",
                    result.conditionName(), result.message(), result.severity());
        } else {
            log.debug("Eval PASSED [{}]", result.conditionName());
        }
    }
}
