package com.healthmetrix.deident.status

import com.healthmetrix.deident.commons.BaseEvent
import com.healthmetrix.deident.commons.ContextualizedEvent
import com.healthmetrix.deident.commons.DateShiftedEvent
import com.healthmetrix.deident.commons.DeidentifiedEvent
import com.healthmetrix.deident.commons.DownloadedEvent
import com.healthmetrix.deident.commons.ErrorEvent
import com.healthmetrix.deident.commons.HarmonizedEvent
import com.healthmetrix.deident.commons.PatientDeduplicatedEvent
import com.healthmetrix.deident.commons.TypeFilteredEvent
import com.healthmetrix.deident.commons.UploadedEvent
import com.healthmetrix.deident.commons.kv
import com.healthmetrix.deident.commons.logger
import com.healthmetrix.deident.persistence.job.api.JobStatus
import com.healthmetrix.deident.status.usecase.UpdateJobStatusUseCase
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class StatusListener(
    private val updateJobStatusUseCase: UpdateJobStatusUseCase,
) {
    @EventListener
    fun onEvent(baseEvent: BaseEvent) {
        val newStatus = when (baseEvent) {
            is DownloadedEvent -> JobStatus.Downloaded // probably ignore in the future
            is TypeFilteredEvent -> JobStatus.TypeFiltered
            is PatientDeduplicatedEvent -> JobStatus.PatientDeduplicated
            is ContextualizedEvent -> JobStatus.Contextualized
            is DateShiftedEvent -> JobStatus.DateShifted
            is DeidentifiedEvent -> JobStatus.Deidentified
            is HarmonizedEvent -> JobStatus.Harmonized
            is UploadedEvent -> JobStatus.Uploaded
            is ErrorEvent -> JobStatus.Error
        }

        if (baseEvent is ErrorEvent) {
            baseEvent.throwables.forEach {
                logger.warn(
                    "Error received {} {}",
                    "jobId" kv baseEvent.context.jobId,
                    "throwable" kv it,
                )
            }
        } else {
            logger.info(
                "Status update received {} {}",
                "jobId" kv baseEvent.context.jobId,
                "status" kv newStatus,
            )
        }

        updateJobStatusUseCase(baseEvent.context.jobId, newStatus)
    }
}
