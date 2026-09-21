package io.github.rinicesiberia.shadowbattle.showdown

/** Pokémon Showdown 使用的 id 规范化规则，供格式、物种和形态映射共用。 */
object ShowdownIdentifiers {
    fun id(value: String): String = value
        .lowercase()
        .filter(Char::isLetterOrDigit)

    fun speciesId(value: String): String = id(value)

    fun formatId(value: String): String = id(value)

    fun same(left: String, right: String): Boolean = id(left) == id(right)
}

/** 官方规则允许集合与当前 Cobblemon 可实例化集合的交集。 */
data class ShowdownSpeciesIntersection(
    val showdownAllowed: Set<String>,
    val cobblemonAvailable: Set<String>
) {
    val allowed: Set<String> = showdownAllowed
        .asSequence()
        .map(ShowdownIdentifiers::speciesId)
        .filter { it in cobblemonAvailable.asSequence().map(ShowdownIdentifiers::speciesId).toSet() }
        .toSet()

    fun contains(species: String): Boolean = ShowdownIdentifiers.speciesId(species) in allowed
}
