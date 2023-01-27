package com.healthmetrix.deident.commons

import ca.uhn.fhir.model.api.annotation.Child
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.r4.model.Base
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import com.github.michaelbull.result.runCatching as catch

fun Base.replace(target: IBase, replacement: IBase?): Result<Unit, Throwable> = binding {
    val toVisit = mutableListOf(this@replace)

    while (toVisit.isNotEmpty()) {
        toVisit += toVisit.removeAt(0)
            .visit(target, replacement)
            .bind()
    }
}

// catching whole body due to extensive HAPI interactions that may throw, plus reflection
private fun Base.visit(target: IBase, replacement: IBase?): Result<List<Base>, Throwable> = catch {
    val toRecurse = mutableListOf<Base>()

    children().forEach { prop ->
        // each property may be a list

        val isList = prop.values.size > 1

        prop.values.forEach { baseValue ->
            if (baseValue === target) {
                if (isList) {
                    throw Exception("Replacing single list element not supported")
                } else {
                    val kProp = findKPropWithName(prop.name)
                        ?: throw Exception("${this::class.java} does not contain child named ${prop.name}")

                    kProp.isAccessible = true
                    kProp.setter.call(this, replacement as Base?)
                }
            } else {
                toRecurse.add(baseValue)
            }
        }
    }

    return toRecurse.let(::Ok)
}

private fun Base.findKPropWithName(name: String): KMutableProperty<*>? =
    this::class.memberProperties
        .mapNotNull {
            it.checkedCast<KMutableProperty<*>>()
        }.singleOrNull { kProp ->
            kProp.findAnnotation<Child>()?.name == name
        }
