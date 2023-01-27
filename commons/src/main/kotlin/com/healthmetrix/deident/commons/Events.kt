package com.healthmetrix.deident.commons

sealed class BaseEvent(val context: IngestionContext)

class DownloadedEvent(context: IngestionContext, val transaction: Transaction) : BaseEvent(context)

class TypeFilteredEvent(context: IngestionContext, val transaction: Transaction) : BaseEvent(context)

class PatientDeduplicatedEvent(context: IngestionContext, val transaction: Transaction) : BaseEvent(context)

class ContextualizedEvent(context: IngestionContext, val transaction: Transaction) : BaseEvent(context)

class DateShiftedEvent(context: IngestionContext, val transaction: Transaction) : BaseEvent(context)

class DeidentifiedEvent(context: IngestionContext, val transaction: Transaction) : BaseEvent(context)

class HarmonizedEvent(context: IngestionContext, val transaction: Transaction) : BaseEvent(context)

class UploadedEvent(context: IngestionContext) : BaseEvent(context)

class ErrorEvent(
    context: IngestionContext,
    val throwables: List<Throwable>,
) : BaseEvent(context) {
    constructor(
        context: IngestionContext,
        throwable: Throwable,
    ) : this(context, listOf(throwable))
}
