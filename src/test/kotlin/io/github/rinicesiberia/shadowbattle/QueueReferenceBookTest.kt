package io.github.rinicesiberia.shadowbattle

import io.github.rinicesiberia.shadowbattle.battle.QueueReferenceBook
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class QueueReferenceBookTest {
    @Test
    fun `waiting entries and references are independently reclaimable`() {
        val book = QueueReferenceBook()
        val participant = UUID.fromString("00000000-0000-0000-0000-000000000001")
        book.putWaiting(QueueReferenceBook.WaitingTeam(participant, emptyList(), "packed", "ranked"))
        book.bindOwner(7, participant)
        book.bindLookup(8, participant)

        assertTrue(book.contains(participant))
        assertEquals(participant, book.owner(7))
        assertNull(book.owner(7))
        assertEquals(participant, book.lookup(8))
        assertEquals("packed", book.claim(participant)?.packed)
        assertFalse(book.contains(participant))
    }

    @Test
    fun `forget participant removes waiting and late response references`() {
        val book = QueueReferenceBook()
        val participant = UUID.fromString("00000000-0000-0000-0000-000000000002")
        val other = UUID.fromString("00000000-0000-0000-0000-000000000003")
        book.putWaiting(QueueReferenceBook.WaitingTeam(participant, emptyList(), "", ""))
        book.bindOwner(1, participant)
        book.bindLookup(2, participant)
        book.bindOwner(3, other)
        book.bindLookup(4, other)

        assertTrue(book.forgetParticipant(participant))
        assertFalse(book.contains(participant))
        assertNull(book.owner(1))
        assertNull(book.lookup(2))
        assertEquals(other, book.owner(3))
        assertEquals(other, book.lookup(4))
        assertFalse(book.forgetParticipant(participant))
    }

    @Test
    fun `clear removes all pending state`() {
        val book = QueueReferenceBook()
        val participant = UUID.randomUUID()
        book.putWaiting(QueueReferenceBook.WaitingTeam(participant, emptyList(), "", ""))
        book.bindOwner(1, participant)
        book.bindLookup(2, participant)
        book.clear()

        assertFalse(book.contains(participant))
        assertNull(book.owner(1))
        assertNull(book.lookup(2))
    }
}
