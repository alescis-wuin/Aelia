# Aelia

Aelia is a Java 21, JavaFX, Maven and AtlantaFX weather dashboard prototype.

This iteration focuses on reproducing the supplied desktop weather mockup with a clean, accessible and maintainable JavaFX implementation. The view is backed by a deterministic simulated provider and does not call any remote API.

## Requirements

- Java 21
- Maven 3.9+
- Git
- Make, optional

## Run

```bash
mvn javafx:run
```

## Test

```bash
mvn test
```

## Verify

```bash
mvn verify
```

## Optional local fonts

The project is configured to use Luciole for general text and Hack for data values when local font files are present under `src/main/resources/fr/alescis/aelia/fonts/`.

```bash
make import-fonts \
  LUCIOLE_ZIP=/path/to/Luciole_webfonts.zip \
  HACK_ZIP=/path/to/Hack-v3.003-ttf.zip
```

The application remains usable with system fallbacks when the fonts are not imported.

## Documentation

Versioned documentation is stored under `docs/V0.3/`, with incremental UI correction notes under `docs/V0.3.8/`.

## Non-modular runtime

This iteration intentionally uses a classpath-based JavaFX launch instead of `module-info.java`. This keeps the first UI-heavy version easier to compile in Maven and IntelliJ while the dashboard components are still evolving.


## V0.3.3 build compatibility

This build includes compatibility helpers for `ApiLimit.limited`, `ApiLimit.unmetered` and `WeatherMetric.maximum`.

## V0.3.8 hero and navigation alignment fix

This iteration refines the latest reported hero and bottom-navigation issues: bottom icons are centered above their labels, the hero date badge now includes the selected city, the weather condition is displayed below the sun icon, and the temperature block is grouped for more consistent vertical centering.
