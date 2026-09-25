package io.github.rinicesiberia.shadowbattle.showdown

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

/** 调用 PS 登录服务器的 HTTP 接口，不记录账号密码或 assertion。 */
class ShowdownLoginHttp(
    private val http: HttpClient = HttpClient.newHttpClient(),
    private val endpoint: URI = URI.create("https://play.pokemonshowdown.com")
) {
    fun login(username: String, password: CharArray, challstr: String): LoginResult {
        return try {
            post("/api/login", mapOf("name" to username, "pass" to password.concatToString(), "challstr" to challstr))
        } finally {
            password.fill('\u0000')
        }
    }

    fun register(username: String, password: CharArray, challstr: String, captcha: String = "pikachu"): LoginResult {
        return try {
            post("/~~showdown/action.php", mapOf(
                "act" to "register", "username" to username, "password" to password.concatToString(),
                "cpassword" to password.concatToString(), "captcha" to captcha, "challstr" to challstr
            ))
        } finally {
            password.fill('\u0000')
        }
    }

    private fun post(path: String, values: Map<String, String>): LoginResult {
        val body = values.entries.joinToString("&") { (key, value) -> "${form(key)}=${form(value)}" }
        val request = HttpRequest.newBuilder(endpoint.resolve(path))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        check(response.statusCode() in 200..299) { "PS 登录服务器返回 HTTP ${response.statusCode()}" }
        val raw = response.body().removePrefix("]")
        val json = JsonParser.parseString(raw).asJsonObject
        val assertion = json.string("assertion")
        val error = json.string("error") ?: json.string("actionerror")
        return LoginResult(assertion, json.objectOrNull("curuser"), error)
    }

    private fun form(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8)

    private fun JsonObject.string(name: String): String? = get(name)?.takeUnless { it.isJsonNull }?.asString
    private fun JsonObject.objectOrNull(name: String): JsonObject? = get(name)?.takeIf { it.isJsonObject }?.asJsonObject

    data class LoginResult(val assertion: String?, val currentUser: JsonObject?, val error: String?)
}
