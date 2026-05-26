# Architecture

Aelia keeps the UI and data-provider integration separated through a small port/service/controller structure.

- `model`: immutable value objects and provider metadata.
- `ports`: remote-provider-ready interfaces.
- `provider.simulation`: local deterministic and random-walk data source.
- `service`: validation and application-level operations.
- `controller`: JavaFX event handling and view state updates.
- `ui`: programmatic JavaFX view and reusable visual components.

The first UI iteration intentionally avoids FXML to keep refactoring simple while the visible structure is still evolving. The provider port can later be implemented for Open-Meteo, Météo-France, OpenAQ or another remote API without changing the controller contract.
