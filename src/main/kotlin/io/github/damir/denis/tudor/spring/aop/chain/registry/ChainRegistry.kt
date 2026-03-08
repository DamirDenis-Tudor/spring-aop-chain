package io.github.damir.denis.tudor.spring.aop.chain.registry

import io.github.damir.denis.tudor.spring.aop.chain.interfaces.Chainable
import io.github.damir.denis.tudor.spring.aop.chain.annotation.ChainStep
import org.springframework.aop.framework.Advised
import org.springframework.aop.support.AopUtils
import org.springframework.context.ApplicationContext
import java.lang.reflect.ParameterizedType

@Suppress("UNCHECKED_CAST")
internal fun ApplicationContext.buildChainMap(): Map<String, ChainNode> =
    getBeansWithAnnotation(ChainStep::class.java)
        .mapNotNull { (beanName, bean) ->
            val targetClass = AopUtils.getTargetClass(bean)

            val annotation = targetClass.getAnnotation(ChainStep::class.java)
                ?: return@mapNotNull null

            require(Chainable::class.java.isAssignableFrom(targetClass)) {
                "@ChainStep on ${targetClass.simpleName} but it does not implement Chainable"
            }

            val (inputType, outputType) = targetClass.resolveChainTypes()
                ?: error("Cannot resolve generics for ${targetClass.simpleName}")

            val id = targetClass.name
            val nextId = annotation.next.takeIf { it != Unit::class }?.java?.name
            val ctx = this

            val invoke: (Any?) -> Any? = { input ->
                val beanInstance = ctx.getBean(beanName)
                val target = if (beanInstance is Advised) beanInstance.targetSource.target else beanInstance
                (target as Chainable<Any?, Any?>).proceed(input)
            }

            id to ChainNode(id, beanName, nextId, inputType, outputType, invoke)
        }
        .toMap()

private fun Class<*>.resolveChainTypes(): Pair<Class<*>, Class<*>>? {
    val chainable = genericInterfaces
        .filterIsInstance<ParameterizedType>()
        .firstOrNull { (it.rawType as? Class<*>) == Chainable::class.java }
        ?: return null

    val args = chainable.actualTypeArguments
    val input = args[0] as? Class<*> ?: return null
    val output = args[1] as? Class<*> ?: return null
    return input to output
}
