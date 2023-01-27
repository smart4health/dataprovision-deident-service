package com.healthmetrix.deident.download

import ca.uhn.fhir.fhirpath.IFhirPath
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.healthmetrix.deident.commons.hmacSha256
import com.healthmetrix.deident.commons.replace
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Reference
import org.springframework.stereotype.Component

@Component
class DeidentifyIdentifiersUseCase(
    private val fhirPath: IFhirPath,
) {

    /**
     * Returns Err(Throwable) when HAPI throws a fit or our hardcoded paths are wrong
     */
    operator fun invoke(bundle: Bundle, secret: ByteArray): Result<Unit, Throwable> = binding {
        /**
         * IDs are strings, but HAPI has more methods as helpers of sorts.
         *
         * Not sure why HAPI conflates ids with more complete references, such
         * as URIs.  The FHIR spec says the following regex matches IDs: [A-Za-z0-9\-\.]{1,64}
         *
         * It is okay for a resource to not have an id, such as the root bundle
         *
         * @see IdType.getIdPart()
         */
        fhirPath.evaluate(bundle, "descendants().ofType(id)", IdType::class.java).forEach { idType ->
            if (idType.hasIdPart()) {
                bundle.replace(idType, idType.idPart.hmacSha256(secret).let(::IdType)).bind()
            }
        }

        /**
         * Identifier is an Element, which has an id, which is handled above
         * Cardinality of Identifier.value is 0..1, so no problem if it isn't there
         * Identifier.assigner is a Reference, which is handled below
         */
        fhirPath.evaluate(bundle, "descendants().ofType(identifier)", Identifier::class.java).forEach { identifier ->
            if (identifier.hasValue()) {
                identifier.value = identifier.value.hmacSha256(secret)
            }
        }

        /**
         * Reference is an Element, which has an id, which is handled above
         * Reference has an Identifier, which is handled above
         * Reference.reference is a very flexible string, but according to the spec
         *   should be an absolute or relative url https://www.hl7.org/fhir/references.html#Reference
         *   The contents of this field, as downloaded from the CHDP, is generally
         *   just the UUID of whatever is referenced, so, for now, assume that's all
         *   we'll see and blindly hmac the .reference
         *
         * Reference SHALL have one of reference, identifier, or display, but display
         *   is cleared out here because it is only used as visual display by the CHDP
         *   and may contain personal info.  This could also be handled by the JSON Filter,
         *   but it doesn't hurt to double check
         */
        fhirPath.evaluate(bundle, "descendants().ofType(Reference)", Reference::class.java).forEach { reference ->
            if (reference.hasReference()) {
                if (reference.referenceElement.hasIdPart()) {
                    reference.reference = reference.referenceElement.idPart.hmacSha256(secret)
                } else {
                    reference.reference = reference.reference.hmacSha256(secret)
                }
            }

            reference.display = null
        }
    }
}
