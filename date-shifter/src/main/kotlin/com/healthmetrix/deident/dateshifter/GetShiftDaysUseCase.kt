package com.healthmetrix.deident.dateshifter

import com.healthmetrix.deident.persistence.dateshift.api.DateShiftRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom

@Component
class GetShiftDaysUseCase(
    private val dateShiftRepository: DateShiftRepository,
    private val secureRandom: SecureRandom,
    @Qualifier("fixedDateShiftDays")
    private val fixedDateShiftDays: Int?,
) {

    @Transactional
    operator fun invoke(d4lId: String): Int = fixedDateShiftDays ?: dateShiftRepository[d4lId] ?: run {
        // nextInt returns int in range [0, 366),
        // add one to get paper-specified [1, 366]
        (secureRandom.nextInt(366) + 1).also {
            dateShiftRepository[d4lId] = it
        }
    }
}
