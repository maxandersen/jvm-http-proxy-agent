package tech.httptoolkit.javaagent

import net.bytebuddy.agent.builder.AgentBuilder
import net.bytebuddy.asm.Advice
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.utility.JavaModule
import net.bytebuddy.matcher.ElementMatchers.*
import tech.httptoolkit.javaagent.asynchttpclient.AsyncHttpClientReturnProxySelectorAdvice
import tech.httptoolkit.javaagent.asynchttpclient.AsyncHttpClientReturnSslContextAdvice
import tech.httptoolkit.javaagent.asynchttpclient.AsyncHttpResetSslEngineFactoryAdvice

// For new clients, we just need to override the properties on the convenient config
// class that contains both proxy & SSL configuration.
class AsyncHttpClientConfigTransformer : MatchingAgentTransformer {
    override fun register(builder: AgentBuilder): AgentBuilder {
        return builder
            .type(
                hasSuperType(named("org.asynchttpclient.AsyncHttpClientConfig"))
            ).and(
                not(isInterface())
            ).transform(this)
    }

    override fun transform(
        builder: DynamicType.Builder<*>,
        typeDescription: TypeDescription,
        classLoader: ClassLoader?,
        module: JavaModule?
    ): DynamicType.Builder<*>? {
        return builder
            .visit(Advice.to(AsyncHttpClientReturnSslContextAdvice::class.java)
                .on(hasMethodName("getSslContext")))
            .visit(Advice.to(AsyncHttpClientReturnProxySelectorAdvice::class.java)
                .on(hasMethodName("getProxyServerSelector")))
    }
}

// For existing classes, we need to hook SSL Handler creation, called for
// every new connection
class AsyncHttpChannelManagerTransformer : MatchingAgentTransformer {
    override fun register(builder: AgentBuilder): AgentBuilder {
        return builder
            .type(
                named("org.asynchttpclient.netty.channel.ChannelManager")
            ).transform(this)
    }

    override fun transform(
        builder: DynamicType.Builder<*>,
        typeDescription: TypeDescription,
        classLoader: ClassLoader?,
        module: JavaModule?
    ): DynamicType.Builder<*>? {
        return builder
            .visit(Advice.to(AsyncHttpResetSslEngineFactoryAdvice::class.java)
                .on(hasMethodName("createSslHandler")))
    }
}