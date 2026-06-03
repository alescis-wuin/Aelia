# Aelia Wealth Tracker

Aelia Wealth Tracker is the new personal wealth tracking workspace for the Aelia repository.

The repository root currently contains a legacy JavaFX weather dashboard prototype. The wealth-tracking work is isolated under `wealth-tracker/` so the new backend/domain foundation can evolve without deleting or destabilizing the existing prototype.

## Product goal

The application tracks personal wealth over time by combining manually entered account balances, investment positions, cash flows, market prices and exchange rates.

The first stable version must answer these questions reliably:

- What is the total net worth on a given date?
- How did an account, envelope, asset class or full portfolio evolve over a chosen period?
- What part of the variation comes from real gains/losses versus deposits, withdrawals or transfers?
- Which values are observed, imported, estimated or stale?

## V1.0 scope

Included:

- local-first backend foundation;
- immutable domain model;
- manual accounts and assets;
- manual account and position snapshots;
- cash flows for deposits, withdrawals, transfers, interest, dividends, fees, taxes and cashback;
- market price and FX-rate records;
- deterministic calculation inputs suitable for future graphs.

Deferred:

- bank synchronization;
- broker synchronization;
- automatic tax computation;
- trading/order execution;
- investment advice;
- hosted multi-user mode;
- real-time market data.

## Workspace layout

```text
wealth-tracker/
├─ pom.xml
├─ backend/
│  └─ domain/
│     ├─ pom.xml
│     └─ src/
└─ docs/
   └─ adr/
```

## Build

```bash
cd wealth-tracker
mvn test
```

## Current implementation status

| Phase | Status | Notes |
|---|---:|---|
| 0 - Project framing | Started | Dedicated workspace and ADRs. |
| 1 - Domain model | Started | Pure Java domain module, no persistence or UI dependency. |
| 2 - Persistence | Not started | PostgreSQL/Flyway later. |
| 3 - REST API | Not started | Spring Boot/OpenAPI later. |
| 4 - Calculation engine | Not started | Uses Phase 1 objects as inputs. |
| 5 - JavaFX app | Not started | Will consume backend/API later. |
