package io.github.rinicesiberia.shadowbattle.battle

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SpeciesLookupIndexTest {
    private data class Form(val id: String)
    private data class Species(val id: String, val standard: Form, val forms: List<Form>)

    @Test
    fun `标准形态和显式形态都建立索引`() {
        val first = Species("alpha", Form("alpha"), listOf(Form("alpha-mega"), Form("alpha-galar")))
        val result = SpeciesLookupIndex.build(
            listOf(first),
            speciesId = { it.id },
            standardForm = { it.standard },
            forms = { it.forms },
            formId = { it.id },
        )
        assertEquals(SpeciesLookupEntry(first, first.standard), result["alpha"])
        assertEquals(SpeciesLookupEntry(first, first.forms[0]), result["alpha-mega"])
    }

    @Test
    fun `重复标识保留首次登记值`() {
        val first = Species("same", Form("standard-one"), listOf(Form("form-one")))
        val second = Species("same", Form("standard-two"), listOf(Form("form-one")))
        val result = SpeciesLookupIndex.build(
            listOf(first, second),
            speciesId = { it.id },
            standardForm = { it.standard },
            forms = { it.forms },
            formId = { it.id },
        )
        assertEquals(first.standard, result["same"]?.form)
        assertEquals(first.forms[0], result["form-one"]?.form)
    }

    @Test
    fun `空物种集合生成空索引`() {
        val result = SpeciesLookupIndex.build(
            emptyList<String>(),
            speciesId = { it },
            standardForm = { it },
            forms = { emptyList() },
            formId = { it },
        )
        assertEquals(emptyMap<String, SpeciesLookupEntry<String, String>>(), result)
    }
}
