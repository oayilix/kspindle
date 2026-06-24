package io.github.oayilix.kspindle.compiler

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Kotlin string literal escaping")
class KotlinStringLiteralTest {

    @Test
    fun `escapes characters that would break generated Kotlin source`() {
        val value = "quote=\" slash=\\ dollar=\$ newline=\n carriage=\r tab=\t backspace=\b"

        assertThat(value.toKotlinStringLiteral())
            .isEqualTo("\"quote=\\\" slash=\\\\ dollar=\\\$ newline=\\n carriage=\\r tab=\\t backspace=\\b\"")
    }

    @Test
    fun `wraps ordinary strings`() {
        assertThat("spanish".toKotlinStringLiteral()).isEqualTo("\"spanish\"")
    }
}
