package fr.alescis.aelia.model;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

/**
 * Describes a data provider without coupling the UI to its implementation.
 */
public record ProviderDescriptor(
        String id,
        String name,
        String attribution,
        boolean remote,
        String version,
        Optional<URI> documentationUri
) {
    public ProviderDescriptor {
        id = requireText(id, "id");
        name = requireText(name, "name");
        attribution = attribution == null ? "" : attribution.trim();
        version = version == null || version.isBlank() ? "unspecified" : version.trim();
        documentationUri = Objects.requireNonNull(documentationUri, "documentationUri");
    }

    public ProviderDescriptor(String id, String name, String attribution, boolean remote) {
        this(id, name, attribution, remote, "unspecified", Optional.empty());
    }

    public ProviderDescriptor(String id, String name, String version, String attribution, Optional<URI> documentationUri) {
        this(id, name, attribution, false, version, documentationUri);
    }

    private static String requireText(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return normalized;
    }
}
