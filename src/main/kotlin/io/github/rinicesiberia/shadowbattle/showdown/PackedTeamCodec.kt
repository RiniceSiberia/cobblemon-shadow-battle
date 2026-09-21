package io.github.rinicesiberia.shadowbattle.showdown

/** Showdown packed team的字段编码，保留空字段和顺序以兼容官方 validator。 */
object PackedTeamCodec {
    fun pack(sets: List<ShowdownSet>): String = sets.joinToString("]") { set ->
        val nickname = set.nickname.ifBlank { set.species }
        val species = if (ShowdownIdentifiers.speciesId(nickname) == ShowdownIdentifiers.speciesId(set.species)) "" else set.species
        listOf(
            nickname,
            species,
            set.item,
            set.ability,
            set.moves.joinToString(","),
            set.nature,
            set.evs,
            set.gender,
            set.ivs,
            if (set.shiny) "S" else "",
            set.level.takeUnless { it == "100" }.orEmpty(),
            listOf(set.happiness, set.hpType, set.hpPower, set.gigantamax, set.dynamaxLevel, set.teraType).joinToString(",")
        ).joinToString("|")
    }

    fun unpack(value: String): List<ShowdownSet> = value.split(']')
        .filter(String::isNotEmpty)
        .map { row ->
            val fields = row.split('|')
            fun field(index: Int) = fields.getOrElse(index) { "" }
            val misc = field(11).split(',')
            ShowdownSet(
                nickname = field(0), species = field(1).ifBlank { field(0) }, item = field(2), ability = field(3),
                moves = field(4).split(',').filter(String::isNotEmpty), nature = field(5), evs = field(6),
                gender = field(7), ivs = field(8), shiny = field(9) == "S", level = field(10).ifBlank { "100" },
                happiness = misc.getOrElse(0) { "" }, hpType = misc.getOrElse(1) { "" }, hpPower = misc.getOrElse(2) { "" },
                gigantamax = misc.getOrElse(3) { "" }, dynamaxLevel = misc.getOrElse(4) { "" }, teraType = misc.getOrElse(5) { "" }
            )
        }
}

data class ShowdownSet(
    val nickname: String = "",
    val species: String,
    val item: String = "",
    val ability: String = "",
    val moves: List<String> = emptyList(),
    val nature: String = "",
    val evs: String = "",
    val gender: String = "",
    val ivs: String = "",
    val shiny: Boolean = false,
    val happiness: String = "",
    val hpType: String = "",
    val hpPower: String = "",
    val level: String = "",
    val gigantamax: String = "",
    val dynamaxLevel: String = "",
    val teraType: String = ""
)
