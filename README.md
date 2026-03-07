# Spring AOP Chain

A Spring Boot auto-configured library for building type-safe processing chains using AOP and annotations.

Define chain steps as Spring beans, wire them together with `@ChainStep`, and let the library handle execution, validation, and error propagation.

## Features

- Declarative chain definition via `@ChainStep` annotation
- Two execution styles: AOP (implicit) and `ChainExecutor` (explicit, type-safe)
- Start execution from any node in the chain
- Multiple disconnected chains in the same application
- Multiple chains can share common steps
- Type compatibility validation at startup
- Cycle detection at startup
- Zero reflection during chain execution
- Auto-configured for Spring Boot 2.x and 3.x

## Installation

Add the dependency to your project:

```kotlin
// Gradle (Kotlin DSL)
implementation("io.github.damir.denis.tudor:spring-aop-chain:0.0.1-SNAPSHOT")
```

```xml
<!-- Maven -->
<dependency>
    <groupId>io.github.damir.denis.tudor</groupId>
    <artifactId>spring-aop-chain</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

Make sure you have `spring-boot-starter-aop` on your classpath.

## Public API

The library exposes three types for end users:

| Type | Description |
|------|-------------|
| `Chainable<I, O>` | Interface each step implements |
| `@ChainStep` | Annotation that wires steps together |
| `ChainExecutor` | Explicit, type-safe chain execution |

All other classes (`ChainNode`, `ChainAspect`, `ChainAutoConfiguration`, registry and validation internals) are `internal` and not part of the public API.

## Quick Start

### 1. Define chain steps

Each step implements `Chainable<I, O>` and is annotated with `@ChainStep`:

```kotlin
@Service
@ChainStep(next = TransformService::class)
class ValidationService : Chainable<String, String> {
    override fun proceed(input: String): String {
        require(input.isNotBlank()) { "Input must not be blank" }
        return input.trim()
    }
}

@Service
@ChainStep(next = PersistService::class)
class TransformService : Chainable<String, Int> {
    override fun proceed(input: String): Int = input.length
}

@Service
@ChainStep
class PersistService : Chainable<Int, Int> {
    override fun proceed(input: Int): Int {
        // save to database...
        return input
    }
}
```

This defines a chain: `ValidationService → TransformService → PersistService`

The last step in a chain omits the `next` parameter (or sets it to `Unit::class`).

### 2. Execute the chain

#### Option A: AOP style (implicit)

Inject the first step and call `proceed()`. The aspect intercepts the call and runs the entire chain:

```kotlin
@RestController
class MyController(
    @Qualifier("validationService")
    private val chain: Chainable<String, String>
) {
    @GetMapping("/process")
    fun process(@RequestParam input: String): String {
        return chain.proceed(input)
    }
}
```

> **Note:** With AOP style, the return type at the injection point must match the first step's declared output type. If the chain transforms types (e.g., `String → Int`), use `ChainExecutor` instead.

#### Option B: ChainExecutor (explicit, type-safe)

Inject `ChainExecutor` and specify the start node and expected output type:

```kotlin
@RestController
class MyController(private val chainExecutor: ChainExecutor) {

    @GetMapping("/process")
    fun process(@RequestParam input: String): Int {
        return chainExecutor.execute<String, Int>(ValidationService::class, input)
    }
}
```

The executor validates both ends at call time:
- The input type `I` must be assignable to the start node's declared input type
- The output type `O` must match the chain's final node output type

If either check fails, an `IllegalStateException` is thrown immediately — no `ClassCastException` at runtime.

## Mid-Chain Entry

You can start execution from any step in the chain, not just the first:

```kotlin
// Starts from TransformService, skipping ValidationService
val result: Int = chainExecutor.execute<String, Int>(TransformService::class, "hello")
```

This works with both `ChainExecutor` and AOP style.

## Shared Steps

Multiple chains can converge on the same step:

```kotlin
@Service
@ChainStep(next = NotificationService::class)
class OrderService : Chainable<Order, Order> { ... }

@Service
@ChainStep(next = NotificationService::class)
class RefundService : Chainable<Refund, Refund> { ... }

@Service
@ChainStep
class NotificationService : Chainable<Refund, String> { ... }
```

## Startup Validation

At application startup, the library validates all chains and fails fast if:

- **Type mismatch between consecutive steps** — a step's output type doesn't match the next step's input type. Checked via `Class.isAssignableFrom`, so subtype relationships are respected.
- **Cycle detected** — a chain forms a circular reference (including self-references). Detected via DFS with a visiting set.
- **Unknown next reference** — a `@ChainStep(next = ...)` points to a class that isn't registered as a chain step bean.

Valid chains are logged at startup:

```
Chain 1:
com.example.ValidationService: class java.lang.String -> class java.lang.String
   -> com.example.TransformService: class java.lang.String -> class java.lang.Integer
   -> com.example.PersistService: class java.lang.Integer -> class java.lang.Integer
```

## Error Handling

Exceptions thrown by any step in the chain are propagated directly to the caller. No wrapping, no swallowing.

## Architecture

```
Startup:
  ApplicationContext
    → buildChainMap()        // discover @ChainStep beans, resolve generics, cache invoke lambdas
    → validateChainTypes()   // fail-fast on type mismatches between consecutive steps
    → detectChainCycles()    // fail-fast on circular references (DFS)
    → logChains()            // log all discovered chains

Runtime (AOP):
  bean.proceed(input)
    → ChainAspect intercepts
    → walks chain via ChainNode.invoke lambdas (zero reflection)

Runtime (Executor):
  chainExecutor.execute<I, O>(StartNode::class, input)
    → validates I against start node input type (reified)
    → validates O against last node output type (reified)
    → walks chain via ChainNode.invoke lambdas (zero reflection)
```

## Limitations

- **Linear chains only** — each step has at most one `next`. No branching or fan-out from a single step. Multiple chains can converge on a shared step, but a step cannot fork into multiple downstream paths.
- **AOP return type** — when using AOP style, the return type at the injection point is the first step's output type, not the chain's final output type. Use `ChainExecutor` for chains that transform types across steps.
- **No conditional routing** — the chain path is fixed at startup via `@ChainStep(next = ...)`. There is no runtime conditional branching.
- **Generics must be concrete** — `Chainable<I, O>` type parameters must be concrete classes (e.g., `String`, `Int`). Parameterized types like `List<String>` cannot be resolved due to JVM type erasure.
- **One chain path per step** — a step can only declare one `next`. To reuse logic across different chains, extract it into a shared service called within the step's `proceed()`.
