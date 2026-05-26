package fr.alescis.aelia.model;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

/**
 * User-facing provider metadata.
 */
public record ProviderDescriptor(
        String name,
        String version,
        String summary,
        boolean networkAccess,
        String accessPolicy,
        Optional<URI> documentationUri
) {
    public ProviderDescriptor {
        name = requireText(name, "name");
        version = requireText(version, "version");
        summary = requireText(summary, "summary");
        accessPolicy = requireText(accessPolicy, "accessPolicy");
        documentationUri = Objects.requireNonNull(documentationUri, "documentationUri");
    }

    private static String requireText(String value, String name) {
        String normalized = Objects.requireNonNull(value, name).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return normalized;
    }
}
