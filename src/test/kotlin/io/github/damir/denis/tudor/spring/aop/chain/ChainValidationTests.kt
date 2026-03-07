package io.github.damir.denis.tudor.spring.aop.chain

import io.github.damir.denis.tudor.spring.aop.chain.aspect.Chainable
import io.github.damir.denis.tudor.spring.aop.chain.aspect.ChainStep
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ChainValidationTests {

    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ChainAutoConfiguration::class.java))

    @ChainStep(next = TypeMismatchStepB::class)
    class TypeMismatchStepA : Chainable<String, Int> {
        override fun proceed(input: String): Int = input.length
    }

    @ChainStep
    class TypeMismatchStepB : Chainable<Double, Double> {
        override fun proceed(input: Double): Double = input * 2
    }

    @Configuration
    @EnableAutoConfiguration
    class TypeMismatchConfig {
        @Bean fun stepA() = TypeMismatchStepA()
        @Bean fun stepB() = TypeMismatchStepB()
    }

    @Test
    fun `should fail on type mismatch between chain steps`() {
        contextRunner
            .withUserConfiguration(TypeMismatchConfig::class.java)
            .run { context -> assertNotNull(context.startupFailure) }
    }

    @ChainStep(next = CycleStepB::class)
    class CycleStepA : Chainable<String, String> {
        override fun proceed(input: String): String = input
    }

    @ChainStep(next = CycleStepA::class)
    class CycleStepB : Chainable<String, String> {
        override fun proceed(input: String): String = input
    }

    @Configuration
    @EnableAutoConfiguration
    class CycleConfig {
        @Bean fun cycleA() = CycleStepA()
        @Bean fun cycleB() = CycleStepB()
    }

    @Test
    fun `should fail when a cycle is detected`() {
        contextRunner
            .withUserConfiguration(CycleConfig::class.java)
            .run { context -> assertNotNull(context.startupFailure) }
    }

    @ChainStep(next = SelfCycleStep::class)
    class SelfCycleStep : Chainable<String, String> {
        override fun proceed(input: String): String = input
    }

    @Configuration
    @EnableAutoConfiguration
    class SelfCycleConfig {
        @Bean fun selfCycle() = SelfCycleStep()
    }

    @Test
    fun `should fail when a step points to itself`() {
        contextRunner
            .withUserConfiguration(SelfCycleConfig::class.java)
            .run { context -> assertNotNull(context.startupFailure) }
    }

    @ChainStep(next = ThreeCycleB::class)
    class ThreeCycleA : Chainable<String, String> {
        override fun proceed(input: String): String = input
    }

    @ChainStep(next = ThreeCycleC::class)
    class ThreeCycleB : Chainable<String, String> {
        override fun proceed(input: String): String = input
    }

    @ChainStep(next = ThreeCycleA::class)
    class ThreeCycleC : Chainable<String, String> {
        override fun proceed(input: String): String = input
    }

    @Configuration
    @EnableAutoConfiguration
    class ThreeCycleConfig {
        @Bean fun a() = ThreeCycleA()
        @Bean fun b() = ThreeCycleB()
        @Bean fun c() = ThreeCycleC()
    }

    @Test
    fun `should fail when a three-node cycle is detected`() {
        contextRunner
            .withUserConfiguration(ThreeCycleConfig::class.java)
            .run { context -> assertNotNull(context.startupFailure) }
    }

    @ChainStep(next = ValidStepB::class)
    class ValidStepA : Chainable<String, String> {
        override fun proceed(input: String): String = input.uppercase()
    }

    @ChainStep
    class ValidStepB : Chainable<String, String> {
        override fun proceed(input: String): String = input.reversed()
    }

    @Configuration
    @EnableAutoConfiguration
    class ValidChainConfig {
        @Bean fun validA() = ValidStepA()
        @Bean fun validB() = ValidStepB()
    }

    @Test
    fun `should start successfully with a valid chain`() {
        contextRunner
            .withUserConfiguration(ValidChainConfig::class.java)
            .run { context -> assertNull(context.startupFailure) }
    }

    @ChainStep(next = SharedTarget::class)
    class BranchA : Chainable<String, String> {
        override fun proceed(input: String): String = input.uppercase()
    }

    @ChainStep(next = SharedTarget::class)
    class BranchB : Chainable<String, String> {
        override fun proceed(input: String): String = input.lowercase()
    }

    @ChainStep
    class SharedTarget : Chainable<String, String> {
        override fun proceed(input: String): String = input.reversed()
    }

    @Configuration
    @EnableAutoConfiguration
    class SharedTargetConfig {
        @Bean fun branchA() = BranchA()
        @Bean fun branchB() = BranchB()
        @Bean fun shared() = SharedTarget()
    }

    @Test
    fun `should start successfully when multiple chains share a target`() {
        contextRunner
            .withUserConfiguration(SharedTargetConfig::class.java)
            .run { context -> assertNull(context.startupFailure) }
    }

    @ChainStep(next = MismatchMiddleB::class)
    class MismatchMiddleA : Chainable<String, Int> {
        override fun proceed(input: String): Int = input.length
    }

    @ChainStep(next = MismatchMiddleC::class)
    class MismatchMiddleB : Chainable<Int, String> {
        override fun proceed(input: Int): String = input.toString()
    }

    @ChainStep
    class MismatchMiddleC : Chainable<Double, Double> {
        override fun proceed(input: Double): Double = input * 2
    }

    @Configuration
    @EnableAutoConfiguration
    class MismatchMiddleConfig {
        @Bean fun a() = MismatchMiddleA()
        @Bean fun b() = MismatchMiddleB()
        @Bean fun c() = MismatchMiddleC()
    }

    @Test
    fun `should fail on type mismatch in the middle of a chain`() {
        contextRunner
            .withUserConfiguration(MismatchMiddleConfig::class.java)
            .run { context -> assertNotNull(context.startupFailure) }
    }
}
