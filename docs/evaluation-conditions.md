# Evaluation Conditions: Design by Contract for AI Agents

## Overview

MockHub implements **evaluation conditions** — formalized sanity checks that encode human domain judgment into automated guards. These prevent AI agents from taking actions that are technically correct but contextually inappropriate.

This feature draws on two intellectual traditions:

1. **Design by Contract** (Bertrand Meyer, 1986) — the idea that software components should operate under explicit preconditions, postconditions, and invariants
2. **Contextual Stewardship** (Nate Jones, 2026) — the argument that the most valuable human skill in an AI-augmented world is encoding domain context into evaluations that agents can't write for themselves

## Why This Matters

AI agents are getting better at executing tasks. They can write code, generate recommendations, and process transactions. But they lack the contextual understanding to know whether a technically correct action is appropriate *right now, in this specific situation*.

Consider: an AI agent can add a ticket to a shopping cart — the API call succeeds, the data is valid. But should it? The event might have already happened. The listing might be expired. The agent doesn't know, because that judgment lives in the domain expert's head.

Evaluation conditions make that judgment explicit and executable.

### The Benchmark Gap

Two studies illustrate the problem:

- **GDP-val** (OpenAI): When AI agents receive full context — the brief, the deliverable format, what "good" looks like — they approach expert-level quality
- **Remote Labor Index** (Scale AI): When agents receive a client brief and must figure out context themselves, the best agent completed only 2.5% of projects at acceptable quality

The gap between these benchmarks is the gap between *task execution* and *contextual understanding*. Evaluation conditions bridge that gap by encoding the context an agent needs but can't derive on its own.

## Design by Contract: Classical Concepts, Modern Application

| DbC Concept | Classical Use (Meyer, 1986) | MockHub Eval Application |
|---|---|---|
| **Precondition** | Caller must satisfy before invoking a method | Before adding to cart: event is in the future, listing is active |
| **Postcondition** | Method guarantees these hold on return | After price prediction: predicted price is plausible relative to current |
| **Invariant** | Always true about the object's state | Cart total always equals sum of item prices |

The key shift: Meyer's contracts protect against **programmer errors**. Our eval conditions protect against **agent contextual blindness** — an agent that is competent but has no awareness of whether its action is appropriate in this specific domain context.

## Eval Conditions in MockHub

### Deterministic Conditions (always run, zero cost)

These are pure domain logic — no AI calls, no external dependencies.

| Condition | DbC Type | What It Checks | What Agent Mistake It Prevents |
|---|---|---|---|
| `EventInFutureCondition` | Precondition | Event date is in the future and status is ACTIVE | Recommending or selling tickets to a past or cancelled event |
| `ListingActiveCondition` | Precondition | Listing status is ACTIVE | Adding an expired or sold listing to the cart |
| `PricePlausibilityCondition` | Postcondition | Predicted price is within reasonable range of current price | AI hallucinating an absurd prediction ($0.01 or $50,000 for a $75 ticket) |
| `RecommendationAvailabilityCondition` | Postcondition | Recommended events are in the future with available tickets | AI recommending sold-out or past events |
| `CartTotalIntegrityCondition` | Invariant | Cart subtotal equals sum of individual item prices | Cart total manipulation or stale price data |

### AI-as-Judge Conditions (opt-in, configurable)

These use a second LLM call to evaluate the primary AI's output. They are more expensive but catch subtler issues.

| Condition | DbC Type | What It Checks | What Agent Mistake It Prevents |
|---|---|---|---|
| `GroundingEvalCondition` | Postcondition | Chat response doesn't fabricate event names, prices, or dates | AI confidently presenting made-up information as fact |

## Architecture

```
com.mockhub.eval/
  EvalCondition.java              -- interface: name(), evaluate(), appliesTo()
  dto/
    EvalResult.java               -- pass/fail/skip with severity and message
    EvalContext.java              -- carries domain data for evaluation
    EvalSeverity.java             -- INFO, WARNING, CRITICAL
    EvalSummary.java              -- aggregation of multiple results
  condition/
    EventInFutureCondition.java   -- deterministic precondition
    ListingActiveCondition.java   -- deterministic precondition
    PricePlausibilityCondition.java  -- deterministic postcondition
    RecommendationAvailabilityCondition.java  -- deterministic postcondition
    CartTotalIntegrityCondition.java  -- deterministic invariant
    GroundingEvalCondition.java   -- AI-as-judge postcondition
  service/
    EvalRunner.java               -- orchestrates conditions, logs results
  config/
    EvalConfig.java               -- eval judge ChatClient, property bindings
```

### Key Design Decisions

1. **Regular interface, not sealed** — `EvalCondition` must be mockable with Mockito for testing, and extensible for students adding their own conditions
2. **Results, not exceptions** — Eval failures return `EvalResult` records. Callers decide what to do (block, filter, log). This keeps decision-making visible in the calling code.
3. **Explicit calls, not AOP** — Every eval invocation is a visible method call. Students read the code and see exactly where judgment happens.
4. **Separate judge ChatClient** — AI-as-judge evals use their own `ChatClient` bean with no tools and no memory, avoiding circular dependencies
5. **AI-judge is opt-in** — Enabled via `mockhub.eval.ai-judge.enabled=true`. Deterministic evals always run.

### Integration Points

Eval conditions are wired into existing services via explicit `EvalRunner.evaluate()` calls:

- **PricePredictionService** — postcondition check on AI-generated prediction before returning
- **RecommendationService** — postcondition check on AI recommendations, filtering out failures
- **ChatService** — postcondition grounding check (informational only, never blocks response)
- **CartTools (MCP)** — precondition check before agent adds items to cart

## Adding a New Condition

To add a new evaluation condition:

1. **Create a test** in `src/test/java/com/mockhub/eval/condition/YourConditionTest.java`
2. **Create the condition** in `src/main/java/com/mockhub/eval/condition/YourCondition.java`
3. **Implement `EvalCondition`** — define `name()`, `appliesTo()`, and `evaluate()`
4. **Annotate with `@Component`** — Spring auto-discovers it and adds it to the `EvalRunner`'s condition list
5. **Add `EvalContext` data if needed** — if your condition needs data not yet in `EvalContext`, add a nullable field and a static factory method

No other configuration is needed. The `EvalRunner` automatically picks up all `@Component` classes implementing `EvalCondition`.

## Configuration

```yaml
mockhub:
  eval:
    ai-judge:
      enabled: false  # Set to true to enable AI-as-judge evals (costs per call)
    price-plausibility:
      min-ratio: 0.1   # Minimum predicted/current price ratio
      max-ratio: 10.0  # Maximum predicted/current price ratio
```

## Further Reading

- Meyer, B. (1986). *Design by Contract*. Technical Report TR-EI-12/CO, Interactive Software Engineering.
- Meyer, B. (1997). *Object-Oriented Software Construction*, 2nd ed. Prentice Hall. (Chapters 11-12 on Design by Contract)
- Jones, N. (2026). ["The agents are getting better. The people deploying them are not."](https://youtu.be/awV2kJzh8zk) (Video on contextual stewardship and the eval gap)
- Scale AI (2026). *Remote Labor Index* — 97.5% agent failure rate on real freelance projects without pre-provided context
- Alibaba (2026). *SWECI Benchmark* — 75% of frontier models break previously working features during long-term code maintenance
