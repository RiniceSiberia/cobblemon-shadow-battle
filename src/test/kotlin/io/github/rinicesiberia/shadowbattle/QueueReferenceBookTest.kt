package io.github.rinicesiberia.shadowbattle

import io.github.rinicesiberia.shadowbattle.battle.QueueReferenceBook
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertThrows
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

    @Test
    fun `发送false撤销对应引用且成功保留到响应领取`() {
        val book = QueueReferenceBook()
        val participant = UUID(0L, 5L)
        val team = QueueReferenceBook.WaitingTeam(participant, emptyList(), "packed", "ranked")

        assertFalse(book.sendLookup(1, participant) { false })
        assertNull(book.lookup(1))
        assertFalse(book.sendOwner(2, participant) { false })
        assertNull(book.owner(2))
        assertFalse(book.sendWaiting(team, 3) { false })
        assertFalse(book.contains(participant))
        assertNull(book.owner(3))

        assertTrue(book.sendWaiting(team, 4) { true })
        assertTrue(book.contains(participant))
        assertEquals(participant, book.owner(4))
    }

    @Test
    fun `发送异常继续传播并保留已登记状态`() {
        val book = QueueReferenceBook()
        val participant = UUID(0L, 6L)
        val failure = IllegalStateException("send")

        assertThrows(IllegalStateException::class.java) {
            book.sendLookup(7, participant) { throw failure }
        }
        assertEquals(participant, book.lookup(7))

        val team = QueueReferenceBook.WaitingTeam(participant, emptyList(), "", "")
        assertThrows(IllegalStateException::class.java) {
            book.sendWaiting(team, 8) { throw failure }
        }
        assertTrue(book.contains(participant))
        assertEquals(participant, book.owner(8))
    }
}
