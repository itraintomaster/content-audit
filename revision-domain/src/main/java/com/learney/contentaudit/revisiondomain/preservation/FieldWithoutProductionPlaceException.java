package com.learney.contentaudit.revisiondomain.preservation;

import com.learney.contentaudit.auditdomain.AuditTarget;

/**
 * F-PUBPROD-R001, inv. 5: la tabla de la corrección declara un campo que no tiene lugar declarado en producción. Nada
 * viaja sin lugar: quien publica se frena y lo nombra.
 */
public class FieldWithoutProductionPlaceException extends RuntimeException {

    private final ScopedField field;

    private final AuditTarget target;

    public FieldWithoutProductionPlaceException(ScopedField field, AuditTarget target) {
        super("el campo «" + describe(field) + "» de " + target + " no tiene lugar declarado en producción");
        this.field = field;
        this.target = target;
    }

    public ScopedField getField() {
        return field;
    }

    public AuditTarget getTarget() {
        return target;
    }

    private static String describe(ScopedField field) {
        if (field == null) {
            return "(ninguno)";
        }
        return field.getElementKind() == null ? field.getPath()
                : field.getPath() + " (" + field.getElementKind() + ")";
    }
}
