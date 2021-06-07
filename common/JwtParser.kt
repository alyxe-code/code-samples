/*
 * Copyright (c) 2021.
 */

package com.kmgi.unicorns.core.network.ktor.utils

import android.os.Build
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.*
import android.util.Base64 as AndroidBase64

class JwtParser(private val json: Json) {
    fun parse(jwt: String): Result {
        val decoded = jwt
            .split(".")
            .mapNotNull(this::decodeChunkOrNull)
            .takeIf { it.isNotEmpty() }
            ?.take(2)

        val header = decoded
            ?.getOrNull(0)
            ?.let { json.decodeFromString<TokenHeader>(it) }

        val payload = decoded
            ?.getOrNull(1)
            ?.let { json.decodeFromString<TokenPayload>(it) }

        return Result(header, payload)
    }

    private fun decodeChunkOrNull(chunk: String): String? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Base64
                .getDecoder()
                .decode(chunk)
                .let(::String)
                .takeIf(String::isNotBlank)
        } else {
            AndroidBase64
                .decode(chunk, AndroidBase64.DEFAULT)
                .let(::String)
                .takeIf(String::isNotBlank)
        }
    } catch (t: Throwable) {
        null
    }

    class Result(
        val header: TokenHeader?,
        val payload: TokenPayload?,
    ) {
        val valid = payload
            ?.expires
            ?.times(1000)
            ?.let(::Date)
            ?.after(Date()) == true
    }

    @Serializable
    class TokenHeader(
        @SerialName("alg")
        val algorithm: String?,
        @SerialName("typ")
        val type: String?,
    ) {
        override fun toString(): String {
            return """
                {
                    "algorithm": $algorithm,
                    "type": $type
                }
            """.trimIndent()
        }
    }

    @Serializable
    class TokenPayload(
        val id: Long?,
        @SerialName("iat")
        val created: Long?,
        @SerialName("exp")
        val expires: Long?,
    ) {
        override fun toString(): String {
            return """
                {
                    "id": $id,
                    "created": ${created?.times(1000)?.let(::Date)},
                    "updated": ${expires?.times(1000)?.let(::Date)}
                }
            """.trimIndent()
        }
    }
}