package fr.alescis.aelia.ui.components;

import java.util.Locale;

/**
 * Configurable raster tile provider descriptor.
 */
record MapTileProvider(String id, String displayName, String urlTemplate, String attribution) {
    private static final MapTileProvider OSM_STANDARD = new MapTileProvider(
            "osm",
            "OpenStreetMap Standard",
            "https://tile.openstreetmap.org/{z}/{x}/{y}.png",
            "© OpenStreetMap contributors"
    );
    private static final MapTileProvider OSM_FRANCE = new MapTileProvider(
            "osm-france",
            "OpenStreetMap France",
            "https://a.tile.openstreetmap.fr/osmfr/{z}/{x}/{y}.png",
            "© OpenStreetMap France · © OpenStreetMap contributors"
    );
    private static final MapTileProvider OSM_FRANCE_HOT = new MapTileProvider(
            "osm-france-hot",
            "OpenStreetMap France HOT",
            "https://a.tile.openstreetmap.fr/hot/{z}/{x}/{y}.png",
            "© OpenStreetMap France · Humanitarian style · © OpenStreetMap contributors"
    );
    private static final MapTileProvider OPEN_TOPO_MAP = new MapTileProvider(
            "opentopomap",
            "OpenTopoMap",
            "https://a.tile.opentopomap.org/{z}/{x}/{y}.png",
            "© OpenTopoMap · © OpenStreetMap contributors"
    );

    static MapTileProvider selected() {
        String overrideUrl = property("aelia.map.tileUrl", "");
        if (!overrideUrl.isBlank()) {
            String name = property("aelia.map.tileProviderName", "Fournisseur personnalisé");
            String attribution = property("aelia.map.tileAttribution", "© OpenStreetMap contributors");
            return new MapTileProvider("custom", name, overrideUrl, attribution);
        }

        String provider = property("aelia.map.tileProvider", "osm")
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace('_', '-')
                .replace('.', '-');
        return switch (provider) {
            case "osm", "openstreetmap", "standard" -> OSM_STANDARD;
            case "osm-fr", "osmfr", "osm-france", "france" -> OSM_FRANCE;
            case "hot", "osm-hot", "osm-france-hot", "humanitarian" -> OSM_FRANCE_HOT;
            case "opentopomap", "topo", "topographic" -> OPEN_TOPO_MAP;
            default -> OSM_STANDARD;
        };
    }

    private static String property(String name, String fallback) {
        String value = System.getProperty(name);
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
