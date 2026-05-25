package fr.seynax.aelia.model;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

/**
 * Provider identity and integration details shown by the UI.
 */
public record ProviderDescriptor(
        String id,
        String displayName,
        String version,
        String accessModel,
        Optional<URI> documentationUri
) {
    public ProviderDescriptor {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(accessModel, "accessModel");
        Objects.requireNonNull(documentationUri, "documentationUri");
        if (id.isBlank() || displayName.isBlank() || version.isBlank() || accessModel.isBlank()) {
            throw new IllegalArgumentException("Provider descriptor fields must not be blank.");
        }
    }
}
