package io.github.rinicesiberia.shadowbattle.showdown

import java.util.concurrent.ConcurrentHashMap

/** 由官方 |formats| 帧维护的规则目录，不预置固定分级名称。 */
class ShowdownFormatCatalog {
    private val formats = ConcurrentHashMap<String, Format>()

    fun update(frame: ShowdownFrame) {
        frame.lines.filter { it.startsWith("|formats|") }.forEach { line ->
            val payload = line.removePrefix("|formats|")
            payload.split('|').forEach { item ->
                val parts = item.split(',')
                val id = parts.firstOrNull()?.trim().orEmpty()
                val name = parts.getOrNull(1)?.trim().orEmpty()
                if (id.isNotBlank()) formats[ShowdownIdentifiers.formatId(id)] = Format(id, name.ifBlank { id }, item.contains("search", true))
            }
        }
    }

    fun all(): List<Format> = formats.values.sortedBy { it.displayName }
    fun find(value: String): Format? = formats[ShowdownIdentifiers.formatId(value)]
    data class Format(val id: String, val displayName: String, val searchable: Boolean)
}
