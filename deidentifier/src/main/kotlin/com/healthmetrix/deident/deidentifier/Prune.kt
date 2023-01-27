package com.healthmetrix.deident.deidentifier

import ca.uhn.fhir.model.api.annotation.Child
import com.healthmetrix.deident.commons.checkedCast
import com.healthmetrix.deident.commons.logger
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Property
import org.hl7.fhir.r4.model.Resource
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

/**
 * Mutate a resource by removing all elements that are not contained
 * in the given set of "live" elements
 *
 * If keepParentsAlive is true, then ancestors of living elements
 * will not be cut (and living children will survive)
 *
 * Future optimization: If keeping parents alive will always be false,
 * then no need to recurse through children, and keeping track of "seen"
 * elements would enable reporting on erroneously removed parents
 *
 * NOTE: is an alive parent with all dead children an edge/error case
 *       that should be reported? Might be confusing when nothing is
 *       reported, but the resource is emptier than expected
 *
 * NOTE: commit 110cf65 has a useful tree visualizer for liveliness
 */
fun Resource.prune(
    liveSet: Set<IBase>,
    keepParentsAlive: Boolean = true,
) {
    (this as Base).prune(
        liveSet = liveSet,
        keepParentsAlive = keepParentsAlive,
        propertyNameStack = listOf(resourceType.name),
    )
}

private fun Base.prune(
    liveSet: Set<IBase>,
    keepParentsAlive: Boolean,
    propertyNameStack: List<String>,
): Liveliness {
    val childrenLiveliness = children().flatMap { prop ->
        if (prop.isList) {
            val (livingChildren, childrenLiveliness) = prop.values.mapIndexed { i, base ->
                base to base.prune(liveSet, keepParentsAlive, propertyNameStack + (prop.name + "[$i]"))
            }.filterIndexed { i, (_, childLiveliness) ->
                if (childLiveliness == Liveliness.HAS_LIVING_CHILDREN) {
                    val currentLocation = (propertyNameStack + (prop.name + "[$i]")).joinToString(separator = ".")
                    val action = if (keepParentsAlive) "keeping" else "cutting"
                    logger.warn("Property $currentLocation has living children but is not alive itself, $action")
                }

                childLiveliness == Liveliness.ALIVE || (keepParentsAlive && childLiveliness == Liveliness.HAS_LIVING_CHILDREN)
            }.unzip()

            setListProperty(prop, livingChildren)

            childrenLiveliness
        } else {
            val childLiveliness = prop.values.singleOrNull()
                ?.prune(liveSet, keepParentsAlive, propertyNameStack + prop.name)

            when (childLiveliness) {
                Liveliness.DEAD -> cut(prop)
                Liveliness.HAS_LIVING_CHILDREN -> {
                    val currentLocation = (propertyNameStack + prop.name).joinToString(separator = ".")
                    val action = if (keepParentsAlive) "keeping" else "cutting"
                    logger.warn("Property $currentLocation has living children but is not alive itself, $action")
                    if (!keepParentsAlive) {
                        cut(prop)
                    }
                }
                Liveliness.ALIVE -> Unit // nothing to do
                null -> Unit // nothing to do
            }

            listOfNotNull(childLiveliness)
        }
    }

    if (childrenLiveliness.isNotEmpty() && childrenLiveliness.all { it == Liveliness.DEAD } && this in liveSet) {
        val propName = propertyNameStack.joinToString(separator = ".")
        logger.warn("Parent $propName is alive but children are dead, may not appear in output")
    }

    return when {
        this in liveSet -> Liveliness.ALIVE
        childrenLiveliness.any { it != Liveliness.DEAD } -> Liveliness.HAS_LIVING_CHILDREN
        else -> Liveliness.DEAD
    }
}

private fun Base.cut(prop: Property) {
    (findKPropWithName(prop.name) ?: return)
        .also { it.isAccessible = true }
        .setter
        .call(this, null as Base?)
}

private fun Base.setListProperty(prop: Property, list: List<Base>) {
    (findKPropWithName(prop.name) ?: return)
        .also { it.isAccessible = true }
        .setter
        .call(this, list)
}

private fun Base.findKPropWithName(name: String): KMutableProperty<*>? =
    this::class.memberProperties
        .mapNotNull {
            it.checkedCast<KMutableProperty<*>>()
        }.singleOrNull { kProp ->
            kProp.findAnnotation<Child>()?.name == name
        }

private enum class Liveliness {
    ALIVE,
    DEAD,
    HAS_LIVING_CHILDREN,
}
