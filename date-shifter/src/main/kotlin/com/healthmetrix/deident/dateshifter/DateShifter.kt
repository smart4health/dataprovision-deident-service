package com.healthmetrix.deident.dateshifter

import com.healthmetrix.deident.commons.ContextualizedEvent
import com.healthmetrix.deident.commons.DateShiftedEvent
import com.healthmetrix.deident.commons.kv
import com.healthmetrix.deident.commons.logger
import com.healthmetrix.deident.commons.tryPublishEvent
import io.micrometer.core.annotation.Timed
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class DateShifter(
    private val getShiftDaysUseCase: GetShiftDaysUseCase,
    private val shiftDateUseCase: ShiftDateUseCase,
    private val rejectResourceUseCase: RejectResourceUseCase,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {

    @EventListener
    @Timed(value = "s4h.date-shifter.timed", description = "Time taken for DateShifter")
    fun onEvent(event: ContextualizedEvent) {
        val shiftDays = getShiftDaysUseCase(event.context.d4lId)

        var newTransaction = event.transaction

        // iterate over the "old" transaction because the new one is being modified
        event.transaction.all.forEach { resource ->
            val outcome = shiftDateUseCase(resource, shiftDays)

            rejectResourceUseCase(
                resource = resource,
                jobId = event.context.jobId,
                outcome = outcome,
            )

            if (outcome != ShiftDateUseCase.Outcome.KEEP) {
                logger.info(
                    "Rejecting resource {} {}",
                    "resourceId" kv resource.id,
                    "jobId" kv event.context.jobId,
                )
                newTransaction = newTransaction.remove(resource)
            }
        }

        DateShiftedEvent(event.context, newTransaction)
            .let(applicationEventPublisher::tryPublishEvent)
    }
}
