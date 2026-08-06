# Aelia

<img width="3436" height="1440" alt="image" src="https://github.com/user-attachments/assets/59650331-da0a-47a2-84a2-7abbbce80647" />
<img width="3436" height="1440" alt="image" src="https://github.com/user-attachments/assets/a32dd2cc-eb5f-4b6f-8c98-c492fa92ca33" />


Aelia is a Java 21 bytecode target, JavaFX, Maven and AtlantaFX weather dashboard prototype.

This iteration keeps the supplied desktop weather mockup visually stable while preparing the codebase for remote weather APIs. The view is still backed by a deterministic simulated provider and does not call any remote API.

## Requirements

- Java 21 or newer
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

Versioned documentation is stored under `docs/V0.3.9/` and `docs/V0.3.10/` for the latest iterations.

## Non-modular runtime

This iteration intentionally uses a classpath-based JavaFX launch instead of `module-info.java`. This keeps the UI-heavy version easier to compile in Maven and IntelliJ while the dashboard components are still evolving.

## V0.3.9 remote API readiness fix

This iteration removes the previously hard-coded gauge progressions from the wind, pressure, air-quality and sun-path cards, introduces typed temporal values in `CurrentWeather`, and keeps formatted strings in the UI formatting layer.
