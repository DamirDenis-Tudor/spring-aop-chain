package io.github.damir.denis.tudor.spring.aop.chain

import io.github.damir.denis.tudor.spring.aop.chain.annotation.ChainStart
import io.github.damir.denis.tudor.spring.aop.chain.annotation.ChainStep
import io.github.damir.denis.tudor.spring.aop.chain.aspect.*
import io.github.damir.denis.tudor.spring.aop.chain.interfaces.Chainable
import io.github.damir.denis.tudor.spring.aop.chain.registry.ChainResult
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service
import kotlin.test.assertEquals

class ChainStartTests {

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

    @Service
    class TestCaller {
        @ChainStart(node = StepA::class)
        fun fullChain(input: String): ChainResult<Int> = ChainResult.Pending

        @ChainStart(node = SingleStep::class)
        fun singleStepChain(input: String): ChainResult<String> = ChainResult.Pending

        @ChainStart(node = StepB::class)
        fun midNodeChain(input: String): ChainResult<Int> = ChainResult.Pending

        @ChainStart(node = StepC::class)
        fun lastStepChain(input: Int): ChainResult<Int> = ChainResult.Pending
    }

    @Configuration
    @EnableAutoConfiguration
    class TestConfig {
        @Bean fun stepA() = StepA()
        @Bean fun stepB() = StepB()
        @Bean fun stepC() = StepC()
        @Bean fun singleStep() = SingleStep()
        @Bean fun testCaller() = TestCaller()
    }

    @Nested
    @SpringBootTest(classes = [TestConfig::class])
    inner class FullChainTests {
        @Autowired lateinit var caller: TestCaller

        @Test
        fun `should execute full chain and return final output`() {
            assertEquals(10, caller.fullChain("hello").result)
        }

        @Test
        fun `should execute single step chain`() {
            assertEquals("olleh", caller.singleStepChain("hello").result)
        }

        @Test
        fun `should execute chain starting from mid node`() {
            assertEquals(10, caller.midNodeChain("hello").result)
        }

        @Test
        fun `should execute last step only`() {
            assertEquals(14, caller.lastStepChain(7).result)
        }
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

    @Service
    class SharedCaller {
        @ChainStart(node = BranchX::class)
        fun branchX(input: String): ChainResult<String> = ChainResult.Pending

        @ChainStart(node = BranchY::class)
        fun branchY(input: String): ChainResult<String> = ChainResult.Pending
    }

    @Configuration
    @EnableAutoConfiguration
    class SharedConfig {
        @Bean fun branchX() = BranchX()
        @Bean fun branchY() = BranchY()
        @Bean fun sharedEnd() = SharedEnd()
        @Bean fun sharedCaller() = SharedCaller()
    }

    @Nested
    @SpringBootTest(classes = [SharedConfig::class])
    inner class SharedTargetTests {
        @Autowired lateinit var caller: SharedCaller

        @Test
        fun `should execute chain through branch X to shared target`() {
            assertEquals("HELLO!", caller.branchX("hello").result)
        }

        @Test
        fun `should execute chain through branch Y to shared target`() {
            assertEquals("hello!", caller.branchY("Hello").result)
        }
    }

    @ChainStep
    class FailingStep : Chainable<String, String> {
        override fun proceed(input: String): String = throw IllegalArgumentException("boom")
    }

    @Service
    class FailingCaller {
        @ChainStart(node = FailingStep::class)
        fun fail(input: String): ChainResult<String> = ChainResult.Pending
    }

    @Configuration
    @EnableAutoConfiguration
    class FailingConfig {
        @Bean fun failingStep() = FailingStep()
        @Bean fun failingCaller() = FailingCaller()
    }

    @Nested
    @SpringBootTest(classes = [FailingConfig::class])
    inner class ExceptionTests {
        @Autowired lateinit var caller: FailingCaller

        @Test
        fun `should propagate exception from chain step`() {
            assertThrows<IllegalArgumentException> { caller.fail("hello") }
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

    @Service
    class IdentityCaller {
        @ChainStart(node = IdentityA::class)
        fun identity(input: String): ChainResult<String> = ChainResult.Pending
    }

    @Configuration
    @EnableAutoConfiguration
    class IdentityConfig {
        @Bean fun identityA() = IdentityA()
        @Bean fun identityB() = IdentityB()
        @Bean fun identityCaller() = IdentityCaller()
    }

    @Nested
    @SpringBootTest(classes = [IdentityConfig::class])
    inner class IdentityTests {
        @Autowired lateinit var caller: IdentityCaller

        @Test
        fun `should pass through identity chain unchanged`() {
            assertEquals("hello", caller.identity("hello").result)
        }
    }

    @ChainStep(next = ToDouble::class)
    class ParseStep : Chainable<String, Int> {
        override fun proceed(input: String): Int = input.toInt()
    }

    @ChainStep
    class ToDouble : Chainable<Int, Double> {
        override fun proceed(input: Int): Double = input * 2.5
    }

    @Service
    class TypeTransformCaller {
        @ChainStart(node = ParseStep::class)
        fun stringToDouble(input: String): ChainResult<Double> = ChainResult.Pending
    }


    @Configuration
    @EnableAutoConfiguration
    class TypeTransformConfig {
        @Bean fun parseStep() = ParseStep()
        @Bean fun toDouble() = ToDouble()
        @Bean fun typeTransformCaller() = TypeTransformCaller()
    }


    @Nested
    @SpringBootTest(classes = [TypeTransformConfig::class])
    inner class TypeTransformTests {
        @Autowired lateinit var caller: TypeTransformCaller

        @Test
        fun `should transform String to Double through chain`() {
            assertEquals(25.0, caller.stringToDouble("10").result)
        }
    }

}
