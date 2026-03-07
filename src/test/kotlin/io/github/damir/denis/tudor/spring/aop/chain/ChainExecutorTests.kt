package io.github.damir.denis.tudor.spring.aop.chain

import io.github.damir.denis.tudor.spring.aop.chain.aspect.Chainable
import io.github.damir.denis.tudor.spring.aop.chain.aspect.ChainStep
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import kotlin.test.assertEquals

class ChainExecutorTests {

    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ChainAutoConfiguration::class.java))

    @ChainStep(next = StepB::class)
    class StepA : Chainable<String, String> {
        override fun proceed(input: String): String = input.uppercase()
    }

    @ChainStep(next = StepC::class)
    class StepB : Chainable<String, Int> {
        override fun proceed(input: String): Int = input.length
    }

    @ChainStep
    class StepC : Chainable<Int, Int> {
        override fun proceed(input: Int): Int = input * 2
    }

    @ChainStep
    class SingleStep : Chainable<String, String> {
        override fun proceed(input: String): String = input.reversed()
    }

    @Configuration
    @EnableAutoConfiguration
    class TestConfig {
        @Bean fun stepA() = StepA()
        @Bean fun stepB() = StepB()
        @Bean fun stepC() = StepC()
        @Bean fun singleStep() = SingleStep()
    }

    @Test
    fun `should execute full chain and return final output`() {
        contextRunner
            .withUserConfiguration(TestConfig::class.java)
            .run { context ->
                val executor = context.getBean(ChainExecutor::class.java)
                val result: Int = executor.execute(StepA::class, "hello")
                assertEquals(10, result)
            }
    }

    @Test
    fun `should execute single step chain`() {
        contextRunner
            .withUserConfiguration(TestConfig::class.java)
            .run { context ->
                val executor = context.getBean(ChainExecutor::class.java)
                val result: String = executor.execute(SingleStep::class, "hello")
                assertEquals("olleh", result)
            }
    }

    @Test
    fun `should execute chain starting from mid node`() {
        contextRunner
            .withUserConfiguration(TestConfig::class.java)
            .run { context ->
                val executor = context.getBean(ChainExecutor::class.java)
                val result: Int = executor.execute(StepB::class, "hello")
                assertEquals(10, result)
            }
    }

    @Test
    fun `should execute last step only`() {
        contextRunner
            .withUserConfiguration(TestConfig::class.java)
            .run { context ->
                val executor = context.getBean(ChainExecutor::class.java)
                val result: Int = executor.execute(StepC::class, 7)
                assertEquals(14, result)
            }
    }

    @Test
    fun `should throw when start node is not in chain map`() {
        contextRunner
            .withUserConfiguration(TestConfig::class.java)
            .run { context ->
                val executor = context.getBean(ChainExecutor::class.java)
                assertThrows<IllegalStateException> {
                    executor.execute<String, String>(UnregisteredStep::class, "hello")
                }
            }
    }

    @Test
    fun `should throw when expected output type does not match chain output`() {
        contextRunner
            .withUserConfiguration(TestConfig::class.java)
            .run { context ->
                val executor = context.getBean(ChainExecutor::class.java)
                assertThrows<IllegalStateException> {
                    executor.execute<String, String>(StepA::class, "hello")
                }
            }
    }

    @ChainStep
    class UnregisteredStep : Chainable<String, String> {
        override fun proceed(input: String): String = input
    }

    @ChainStep(next = SharedEnd::class)
    class BranchX : Chainable<String, String> {
        override fun proceed(input: String): String = input.uppercase()
    }

    @ChainStep(next = SharedEnd::class)
    class BranchY : Chainable<String, String> {
        override fun proceed(input: String): String = input.lowercase()
    }

    @ChainStep
    class SharedEnd : Chainable<String, String> {
        override fun proceed(input: String): String = "$input!"
    }

    @Configuration
    @EnableAutoConfiguration
    class SharedTargetConfig {
        @Bean fun branchX() = BranchX()
        @Bean fun branchY() = BranchY()
        @Bean fun sharedEnd() = SharedEnd()
    }

    @Test
    fun `should execute chain through branch X to shared target`() {
        contextRunner
            .withUserConfiguration(SharedTargetConfig::class.java)
            .run { context ->
                val executor = context.getBean(ChainExecutor::class.java)
                val result: String = executor.execute(BranchX::class, "hello")
                assertEquals("HELLO!", result)
            }
    }

    @Test
    fun `should execute chain through branch Y to shared target`() {
        contextRunner
            .withUserConfiguration(SharedTargetConfig::class.java)
            .run { context ->
                val executor = context.getBean(ChainExecutor::class.java)
                val result: String = executor.execute(BranchY::class, "Hello")
                assertEquals("hello!", result)
            }
    }

    @ChainStep
    class FailingStep : Chainable<String, String> {
        override fun proceed(input: String): String = throw IllegalArgumentException("boom")
    }

    @Configuration
    @EnableAutoConfiguration
    class FailingConfig {
        @Bean fun failingStep() = FailingStep()
    }

    @Test
    fun `should propagate exception from chain step`() {
        contextRunner
            .withUserConfiguration(FailingConfig::class.java)
            .run { context ->
                val executor = context.getBean(ChainExecutor::class.java)
                assertThrows<IllegalArgumentException> {
                    executor.execute<String, String>(FailingStep::class, "hello")
                }
            }
    }

    @ChainStep(next = IdentityB::class)
    class IdentityA : Chainable<String, String> {
        override fun proceed(input: String): String = input
    }

    @ChainStep
    class IdentityB : Chainable<String, String> {
        override fun proceed(input: String): String = input
    }

    @Configuration
    @EnableAutoConfiguration
    class IdentityConfig {
        @Bean fun identityA() = IdentityA()
        @Bean fun identityB() = IdentityB()
    }

    @Test
    fun `should pass through identity chain unchanged`() {
        contextRunner
            .withUserConfiguration(IdentityConfig::class.java)
            .run { context ->
                val executor = context.getBean(ChainExecutor::class.java)
                val result: String = executor.execute(IdentityA::class, "hello")
                assertEquals("hello", result)
            }
    }
}
