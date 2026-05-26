# Simulation model

The local provider uses numeric random-walk states with a slow recovery toward a cyclic daily target. Each metric has a realistic minimum, maximum, volatility, recovery speed and cycle amplitude.

Textual values are simulated separately:

- `condition` changes slowly among compact weather states;
- `sunrise` and `sunset` use a deterministic local seasonal approximation.

The simulation is intentionally provider-shaped. It exposes the same operations expected from a future remote API adapter:

- list supported data;
- list limits;
- read current value;
- subscribe;
- unsubscribe.
