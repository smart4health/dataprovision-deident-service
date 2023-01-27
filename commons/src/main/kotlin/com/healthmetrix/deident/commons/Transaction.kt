package com.healthmetrix.deident.commons

import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Resource

data class Transaction(
    val toUpdate: List<Resource> = emptyList(),
    val toCreate: List<Resource> = emptyList(),
) {
    fun update(resource: Resource) = copy(toUpdate = toUpdate + resource)

    fun create(resource: Resource) = copy(toCreate = toCreate + resource)

    fun remove(resource: Resource) = copy(
        toUpdate = toUpdate - resource,
        toCreate = toCreate - resource,
    )

    val all: List<Resource>
        get() = toUpdate + toCreate

    // TODO new class TransactionBuilder
    fun asBundle() = Bundle().apply {
        type = Bundle.BundleType.TRANSACTION
        toUpdate.forEach { resource ->
            addEntry().also { entry ->
                entry.resource = resource
                entry.request = Bundle.BundleEntryRequestComponent().apply {
                    method = Bundle.HTTPVerb.PUT
                    url = resource.id
                }
            }
        }

        toCreate.forEach { resource ->
            addEntry().also { entry ->
                entry.resource = resource
                entry.request = Bundle.BundleEntryRequestComponent().apply {
                    method = Bundle.HTTPVerb.POST
                    url = resource.resourceType.name
                }
            }
        }
    }
}
