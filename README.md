# Aelia

Aelia is a Java 21 bytecode target, JavaFX, Maven and AtlantaFX weather dashboard prototype.

This iteration keeps the supplied desktop weather mockup visually stable while supporting both the deterministic simulated provider and an optional Open-Meteo remote provider.

## Requirements

- Java 21 or newer
- Maven 3.9+
- Git
- Make, optional

## Run

```bash
mvn javafx:run
```


## Remote provider

```bash
mvn -Daelia.weather.provider=openmeteo \
  -Daelia.openmeteo.latitude=49.4431 \
  -Daelia.openmeteo.longitude=1.0993 \
  -Daelia.openmeteo.city=Rouen \
  -Daelia.openmeteo.country=France \
  -Daelia.openmeteo.timezone=Europe/Paris \
  javafx:run
```

Environment variables are also supported when Maven properties are left blank, for example `AELIA_WEATHER_PROVIDER=openmeteo mvn javafx:run`.

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

Versioned documentation is stored under `docs/V0.3.13/` and `docs/V0.3.14/` for the latest remote-provider iterations.

## Non-modular runtime

This iteration intentionally uses a classpath-based JavaFX launch instead of `module-info.java`. This keeps the UI-heavy version easier to compile in Maven and IntelliJ while the dashboard components are still evolving.

## V0.3.9 remote API readiness fix

This iteration removes the previously hard-coded gauge progressions from the wind, pressure, air-quality and sun-path cards, introduces typed temporal values in `CurrentWeather`, and keeps formatted strings in the UI formatting layer.


## V0.3.14 Open-Meteo diagnostics and Maven forwarding

This iteration forwards Maven `-D` settings to the JavaFX runtime, makes strict Open-Meteo mode load remote data synchronously, and prints provider diagnostics so remote failures are visible in the console.
