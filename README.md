# genai-java-agents

## Why this agent exists

A refund decision is a poor place to trust free-form model text. The amount, the eligibility window, and whether an order was delivered are facts the system already has. The model’s job is to gather those facts and propose a decision. The system’s job is to type that proposal and refuse to continue if the model will not stop.

`agent-1` is that boundary, and nothing more. It is the first lab in a series that will add a harness, guardrails, approvals, and later a second agent. Those later labs only make sense if the basic loop is visible and tested: the model asks for a tool, this process runs it, and the result goes back, until the model returns a `RefundDecision` or hits a hard cap on model calls.

The model does not issue the refund. `checkRefundEligibility` only reports whether an order qualifies and the maximum amount. A malformed or out-of-range reply is sent back with the validation error so the model can repair it. That is the whole motivation of this agent: a loop you can read, a contract you can validate, and a stop condition that does not depend on the model cooperating.

## Run

Requires JDK 21+ and Ollama with `qwen2.5:7b`.

```bash
export JAVA_HOME=/Users/soumikmukherjee/softwares/jdk25/jdk-25.0.4.1.jdk/Contents/Home
mvn test
mvn spring-boot:run
```

```bash
curl -s localhost:8080/api/refund-decisions \
  -H 'content-type: application/json' \
  -d '{"orderId":"ORD-1001","customerNote":"The speaker arrived cracked"}'
```

Sample orders: `ORD-1001` (eligible), `ORD-1002` (outside the 30-day window), `ORD-1003` (cancelled).
