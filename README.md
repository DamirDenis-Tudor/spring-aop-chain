# Spring AOP Chain

A Spring Boot auto-configured library for building type-safe processing chains using AOP and annotations.

Define chain steps as Spring beans, wire them together with `@ChainStep`, and let the library handle execution, validation, and error propagation.

## Features

- Declarative chain definition via `@ChainStep` annotation
- Two execution styles: `@ChainStart` (recommended) and AOP on `Chainable.proceed()`
- Start execution from any node in the chain
- Multiple disconnected chains in the same application
- Multiple chains can share common steps
- Type compatibility validation at startup
- Cycle detection at startup
- `@ChainStart` input/output type validation at startup
- Zero reflection during chain execution

## Installation

Requires Java 17+ and Spring Boot 3.x.

Add the dependency to your project:

```kotlin
// Gradle (Kotlin DSL)
implementation("io.github.damirdenis-tudor:spring-aop-chain:0.0.1")
```

```xml
<!-- Maven -->
<dependency>
    <groupId>io.github.damirdenis-tudor</groupId>
    <artifactId>spring-aop-chain</artifactId>
    <version>0.0.1</version>
</dependency>
```

Make sure you have `spring-boot-starter-aop` on your classpath.

## Public API

| Type | Description |
|------|-------------|
| `Chainable<I, O>` | Interface each step implements |
| `@ChainStep` | Annotation that wires steps together |
| `@ChainStart` | Annotation that triggers chain execution from a method |
| `ChainResult<T>` | Sealed class wrapping the chain's output |

All other classes (`ChainNode`, `ChainAspect`, `ChainStartAspect`, `ChainAutoConfiguration`, `ChainExecutor`, registry and validation internals) are `internal` and not part of the public API.

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

#### Option A: `@ChainStart` (recommended)

Annotate any method with `@ChainStart` and return `ChainResult<T>`. The aspect intercepts the call, runs the chain, and replaces the return value:

```kotlin
@RestController
class MyController {

    @GetMapping("/process")
    @ChainStart(node = ValidationService::class)
    fun process(@RequestParam input: String): ChainResult<Int> = ChainResult.Pending
}
```

The method body is never executed — `ChainResult.Pending` is just a placeholder. The aspect runs the chain starting from `ValidationService` and returns a `ChainResult.Success` with the final output.

Access the result:

```kotlin
val response: ChainResult<Int> = controller.process("hello")
val value: Int = response.result // 5
```

At startup, the library validates:
- The method's first parameter type matches the start node's input type
- The `ChainResult<T>` type parameter matches the chain's final output type
- The return type is `ChainResult<*>`

If any check fails, the application fails to start.

#### Option B: AOP on `Chainable.proceed()` (implicit)

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

> **Note:** With this style, the return type at the injection point must match the first step's declared output type. If the chain transforms types (e.g., `String → Int`), use `@ChainStart` instead.

## `ChainResult<T>`

`ChainResult` is a sealed class with two subtypes:

```kotlin
sealed class ChainResult<out T> {
    data class Success<T>(val value: T) : ChainResult<T>()
    object Pending : ChainResult<Nothing>()

    val result: T  // convenience getter — throws on Pending
}
```

- `ChainResult.Pending` — placeholder used in `@ChainStart` method bodies
- `ChainResult.Success` — returned by the aspect after chain execution

When serialized as JSON (e.g., from a Spring controller), only `result` is included:

```json
{ "result": 42 }
```

## Mid-Chain Entry

You can start execution from any step in the chain, not just the first:

```kotlin
@ChainStart(node = TransformService::class)
fun fromMiddle(input: String): ChainResult<Int> = ChainResult.Pending
```

This skips `ValidationService` and starts from `TransformService`.

## Shared Steps

Multiple chains can converge on the same step:

```kotlin
@Service
@ChainStep(next = NotificationService::class)
class OrderService : Chainable<Order, Notification> { ... }

@Service
@ChainStep(next = NotificationService::class)
class RefundService : Chainable<Refund, Notification> { ... }

@Service
@ChainStep
class NotificationService : Chainable<Notification, String> { ... }
```

## Startup Validation

At application startup, the library validates all chains and `@ChainStart` methods. The application fails fast if:

- **Type mismatch between consecutive steps** — a step's output type doesn't match the next step's input type. Checked via `Class.isAssignableFrom`, so subtype relationships are respected.
- **Cycle detected** — a chain forms a circular reference (including self-references). Detected via DFS.
- **Unknown next reference** — a `@ChainStep(next = ...)` points to a class that isn't registered as a chain step bean.
- **`@ChainStart` return type is not `ChainResult<*>`** — the annotated method must return `ChainResult<T>`.
- **`@ChainStart` input type mismatch** — the method's first parameter type doesn't match the start node's expected input type.
- **`@ChainStart` output type mismatch** — the `T` in `ChainResult<T>` doesn't match the chain's final node output type.

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
    → buildChainMap()                // discover @ChainStep beans, resolve generics, cache invoke lambdas
    → validateChainTypes()           // fail-fast on type mismatches between consecutive steps
    → detectChainCycles()            // fail-fast on circular references (DFS)
    → validateChainStartMethods()    // fail-fast on @ChainStart input/output type mismatches
    → logChains()                    // log all discovered chains

Runtime (@ChainStart):
  method(input)
    → ChainStartAspect intercepts
    → walks chain via ChainNode.invoke lambdas (zero reflection)
    → returns ChainResult.Success(finalOutput)

Runtime (AOP on Chainable.proceed):
  bean.proceed(input)
    → ChainAspect intercepts
    → walks chain via ChainNode.invoke lambdas (zero reflection)
    → returns final output directly
```

## Limitations

- **Linear chains only** — each step has at most one `next`. No branching or fan-out from a single step. Multiple chains can converge on a shared step, but a step cannot fork into multiple downstream paths.
- **AOP return type** — when using AOP style on `Chainable.proceed()`, the return type at the injection point is the first step's output type, not the chain's final output type. Use `@ChainStart` for chains that transform types across steps.
- **No conditional routing** — the chain path is fixed at startup via `@ChainStep(next = ...)`. There is no runtime conditional branching.
- **Generics must be concrete** — `Chainable<I, O>` type parameters must be concrete classes (e.g., `String`, `Int`). Parameterized types like `List<String>` cannot be resolved due to JVM type erasure.
- **One chain path per step** — a step can only declare one `next`. To reuse logic across different chains, extract it into a shared service called within the step's `proceed()`.
