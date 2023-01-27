package com.healthmetrix.deident

import com.healthmetrix.deident.commons.ApiResponse
import com.healthmetrix.deident.commons.HmacByteArrayDecodeException
import com.healthmetrix.deident.commons.asEntity
import com.healthmetrix.deident.commons.logger
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class GlobalAdvice {
    @ExceptionHandler(HmacByteArrayDecodeException::class)
    fun handleHmacByteArrayDecodeException(ex: HmacByteArrayDecodeException): ResponseEntity<ApiResponse> {
        logger.info("Exception while decoding byte array", ex)

        return object : ApiResponse {
            val message = "Failed to decode base64 string"
            override val status = HttpStatus.BAD_REQUEST
        }.asEntity()
    }
}
