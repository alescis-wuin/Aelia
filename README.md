# Aelia

A Java 21, JavaFX, Maven and AtlantaFX starter for a weather utility application.

The first implementation uses a local simulated provider instead of a remote weather API. The provider exposes the same contract expected from future remote adapters: supported data, API limits, current values, stream subscriptions and unsubscriptions.

## Features

- JavaFX MVC structure with a provider-facing port.
- AtlantaFX dark theme plus a high-contrast custom stylesheet.
- Simulated weather, air-quality, UV and pollen data with realistic bounds and smooth variation.
- Current-value requests for any supported metric.
- Live subscriptions with a configurable interval.
- Local API-limit and supported-data tables.
- Keyboard-friendly controls, tooltips and JavaFX accessibility metadata.
- Maven build, unit tests, GitHub Actions workflow and Git/GitHub bootstrap script.

## Requirements

- Java 21
- Maven 3.9+
- Git
- GitHub CLI `gh` only when remote repository creation and branch protection are required
- Make, optional but convenient

## Run

```bash
mvn javafx:run
```

## Test

```bash
mvn test
```

## Build

```bash
mvn verify
```

## GitHub bootstrap

```bash
./init-project.sh
```

The script can update Maven coordinates, Java packages and repository metadata, initialize Git, create the remote GitHub repository, push `main`, create `testing` and `develop`, set `develop` as the default branch and protect all three branches with pull-request rules.

## Make shortcuts

```bash
make run
make test
make verify
make init
make feature-start name=my-change
make feature-push
```

## Documentation

Project documentation is stored under `docs/V0.1/`.
