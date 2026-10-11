package com.learney.contentaudit.revisiondomain.publication;

import com.learney.contentaudit.revisiondomain.preservation.FieldWithoutProductionPlaceException;

/**
 * F-PUBPROD-R001 y R002: arma la lista de publicación. Por cada ejercicio o tema, los campos que sus pasos declaran
 * según la tabla de la corrección, llevados a producción según su lugar, y sólo los que cambiaron.
 */
public interface PublicationManifestBuilder {

    /**
     * @throws FieldWithoutProductionPlaceException si un campo declarado no tiene lugar en producción
     */
    PublicationManifest build(PublicationManifestRequest request);
}
