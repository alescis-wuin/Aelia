# ADR 0001 - Architecture boundaries

## Status

Accepted.

## Context

Aelia Wealth Tracker must support a Java backend, a JavaFX desktop client, and a later Web client. The first repository state still contains a JavaFX weather prototype at the root, while the new product direction is wealth tracking.

Financial tracking also requires long-lived, testable domain rules. UI, persistence, HTTP and provider choices are expected to change more often than the money, asset, account and cash-flow concepts.

## Decision

The wealth tracker is introduced as a dedicated Maven workspace under `wealth-tracker/`.

The initial module is `aelia-wealth-domain` and contains only pure Java domain objects and tests. It must not depend on:

- JavaFX;
- Spring Boot;
- PostgreSQL drivers;
- HTTP clients;
- market-data SDKs;
- external banking or broker APIs.

Future modules will depend inward on the domain module:

```text
JavaFX/Web/API/Persistence/Provider adapters -> Application services -> Domain
```

## Consequences

Positive:

- the domain can be tested quickly and deterministically;
- financial calculations can be reused by desktop, REST and future Web code;
- UI and persistence choices can evolve without rewriting core concepts;
- the existing weather prototype can remain untouched while the new product foundation starts.

Trade-offs:

- the repository temporarily contains two product directions;
- some integration work is deferred until persistence and REST modules exist;
- module boundaries add a small amount of Maven configuration.
