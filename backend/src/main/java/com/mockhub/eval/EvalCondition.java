package com.mockhub.eval;

import com.mockhub.eval.dto.EvalContext;
import com.mockhub.eval.dto.EvalResult;

public interface EvalCondition {

    String name();

    EvalResult evaluate(EvalContext context);

    boolean appliesTo(EvalContext context);
}
