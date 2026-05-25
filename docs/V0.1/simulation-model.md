# Simulation Model V0.1

## Scope

The simulator generates realistic bounded values for weather, air quality, UV and pollen metrics. It is deterministic only at the contract level: values always stay within declared metric ranges, while their trajectory changes over time.

## Numeric values

Each numeric metric owns a state with:

- a current value;
- a moving target;
- a velocity;
- bounded random perturbations;
- an attraction factor pulling the value back toward the target.

This produces smooth motion instead of unrelated random values at each refresh.

## Categorical values

Weather condition values use a small state machine. Conditions change slowly and adjacent states are favored, avoiding abrupt jumps from clear sky to storm at every sample.

## Time values

Sunrise and sunset are approximated from the day of year. They are not location-aware in V0.1 and should be replaced by a remote-provider value or a geospatial astronomical calculation in a later version.
