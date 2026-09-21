package io.github.rinicesiberia.shadowbattle

import io.github.rinicesiberia.shadowbattle.showdown.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ShowdownBridgeContractTest {
    @Test
    fun `id removes punctuation and normalizes case`() {
        assertEquals("mr rime".filter(Char::isLetterOrDigit), ShowdownIdentifiers.id("Mr. Rime"))
        assertTrue(ShowdownIdentifiers.same("Alolan-Ninetales", "alolan ninetales"))
    }

    @Test
    fun `intersection only keeps entries present on both sides`() {
        val result = ShowdownSpeciesIntersection(setOf("Pikachu", "MissingNo"), setOf("pikachu"))
        assertEquals(setOf("pikachu"), result.allowed)
    }

    @Test
    fun `packed team round trips empty fields`() {
        val team = listOf(ShowdownSet(species = "Pikachu", moves = listOf("thunderbolt")))
        val decoded = PackedTeamCodec.unpack(PackedTeamCodec.pack(team)).single()
        assertEquals("Pikachu", decoded.nickname)
        assertEquals("Pikachu", decoded.species)
        assertEquals(listOf("thunderbolt"), decoded.moves)
        assertEquals("100", decoded.level)
    }

    @Test
    fun `commands reject protocol delimiters`() {
        assertEquals("|/accept badname", ShowdownProtocol.accept("bad|name\n"))
        assertEquals("battle-1|/forfeit", ShowdownProtocol.forfeit("battle-1"))
    }
}
