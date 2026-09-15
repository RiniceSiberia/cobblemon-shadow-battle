package io.github.rinicesiberia.shadowbattle.battle

/** 远端 Showdown 标识到 Cobblemon 物种/形态的查找值。 */
data class SpeciesLookupEntry<TSpecies, TForm>(
    val species: TSpecies,
    val form: TForm,
)

/** 构建稳定的物种索引，重复标识保留第一次登记的形态。 */
object SpeciesLookupIndex {
    @JvmStatic
    fun <TSpecies, TForm> build(
        species: Iterable<TSpecies>,
        speciesId: (TSpecies) -> String,
        standardForm: (TSpecies) -> TForm,
        forms: (TSpecies) -> Iterable<TForm>,
        formId: (TForm) -> String,
    ): Map<String, SpeciesLookupEntry<TSpecies, TForm>> {
        val result = LinkedHashMap<String, SpeciesLookupEntry<TSpecies, TForm>>()
        for (template in species) {
            result.putIfAbsent(speciesId(template), SpeciesLookupEntry(template, standardForm(template)))
            for (form in forms(template)) {
                result.putIfAbsent(formId(form), SpeciesLookupEntry(template, form))
            }
        }
        return result
    }
}
