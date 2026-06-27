package com.app.pegsolitaire.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PlaceholderTest {
    @Test
    fun placeholderRetainsItsName() {
        val placeholder = Placeholder(name = "english-33")

        assertThat(placeholder.name).isEqualTo("english-33")
    }
}
