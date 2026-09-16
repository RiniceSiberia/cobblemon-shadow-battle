package io.github.rinicesiberia.shadowbattle.battle

/** 两位本地参与者的队伍领取结果，允许任意一侧缺失。 */
data class LocalMatchClaims<T>(val first: T?, val second: T?) {
    companion object {
        fun <I, T> take(firstId: I, secondId: I, claim: (I) -> T?): LocalMatchClaims<T> {
            val first = claim(firstId)
            val second = claim(secondId)
            return LocalMatchClaims(first, second)
        }
    }
}
