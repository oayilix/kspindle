package com.spi.framework.compiler

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

/**
 * Provider for [ServiceProviderProcessor], registered via META-INF/services
 * for automatic discovery by KSP.
 * [ServiceProviderProcessor] 的提供者，通过 META-INF/services 注册以供 KSP 自动发现。
 */
class ServiceProviderProcessorProvider : SymbolProcessorProvider {

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return ServiceProviderProcessor(
            options = environment.options,
            codeGenerator = environment.codeGenerator,
            logger = environment.logger
        )
    }
}
