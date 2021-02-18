package kgit.data

import assertk.Assert
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull

fun Assert<Index>.containsExactKeys(vararg keys: String) {
    given { actual ->
        assertThat(actual.getSize()).isEqualTo(keys.size)
        keys.forEach { key ->
            assertThat(actual[key]).isNotNull()
        }
    }
}