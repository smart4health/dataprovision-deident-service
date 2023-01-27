package com.healthmetrix.deident.commons

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

fun String.sha256(): String {
    return MessageDigest.getInstance("SHA-256")
        .digest(toByteArray())
        .joinToString("") { "%02x".format(it) }
}

// seems like Mac::getInstance returns a new object each time, meaning
// this method is thread safe
fun String.hmacSha256(secret: ByteArray): String = with(Mac.getInstance("HmacSHA256")) {
    init(SecretKeySpec(secret, algorithm))
    val cipherText = doFinal(toByteArray())
    reset()
    cipherText.joinToString("") { b -> "%02X".format(b) }
}

fun String.decodeBase64(): Result<ByteArray, Base64DecodeException> {
    return Base64.getDecoder().runCatching {
        decode(this@decodeBase64)
    }.mapError(::Base64DecodeException)
}

class Base64DecodeException(ex: Throwable) : RuntimeException("Failed to decode base64", ex)

fun String.nonEmpty() = when {
    this.isEmpty() -> IllegalArgumentException("String is empty").err()
    else -> ok()
}
