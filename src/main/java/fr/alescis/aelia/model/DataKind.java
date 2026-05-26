package fr.alescis.aelia.model;

/**
 * Describes either a provider value representation or a future temporal data scope.
 */
public enum DataKind {
    NUMERIC,
    TEXT,
    TEMPORAL,
    CURRENT,
    HOURLY,
    DAILY,
    OBSERVED,
    FORECAST
}
