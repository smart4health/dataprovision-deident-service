package com.healthmetrix.deident.commons

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.flatMap
import com.github.michaelbull.result.mapError

class HmacByteArrayDecodeException(ex: Throwable) : Exception(ex)

fun String.decodeUserSecret(): Result<ByteArray, HmacByteArrayDecodeException> =
    nonEmpty()
        .flatMap(String::decodeBase64)
        .mapError(::HmacByteArrayDecodeException)
