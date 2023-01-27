package com.healthmetrix.deident.download

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.flatMap
import com.healthmetrix.deident.commons.decodeBase64
import com.healthmetrix.deident.commons.nonEmpty
import com.github.michaelbull.result.runCatching as catch

fun String.decodeD4lId(): Result<String, Throwable> {
    return nonEmpty()
        .flatMap(String::decodeBase64)
        .flatMap { byteArray ->
            catch {
                byteArray.toString(Charsets.UTF_8)
            }
        }
}
