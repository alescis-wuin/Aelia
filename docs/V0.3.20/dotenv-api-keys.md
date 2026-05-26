# V0.3.20 dotenv API key configuration

## Objective

This iteration centralizes remote provider credentials in a local `.env` file while preserving the existing override order for command-line and operating-system configuration.

## Resolution order

Aelia resolves each setting in this order:

1. Java system property, for example `-Daelia.weatherapi.apiKey=...`.
2. Operating-system environment variable, for example `AELIA_WEATHERAPI_API_KEY=...`.
3. Project-local dotenv file, by default `.env` in the working directory.

The dotenv path can be customized with:

```bash
-Daelia.env.path=/absolute/or/relative/path/to/.env
```

or:

```bash
AELIA_ENV_PATH=/absolute/or/relative/path/to/.env
```

## Files

- `.env.example` documents all supported keys.
- `.gitignore` excludes `.env` and `.env.*` while keeping `.env.example` versionable.
- `EnvironmentSettings` is the single settings resolver used by diagnostics, provider selection and Open-Meteo/fallback configuration.

## Supported dotenv keys

```dotenv
AELIA_WEATHER_PROVIDER=auto

AELIA_OPENMETEO_LATITUDE=48.8566
AELIA_OPENMETEO_LONGITUDE=2.3522
AELIA_OPENMETEO_CITY=Paris
AELIA_OPENMETEO_COUNTRY=France
AELIA_OPENMETEO_TIMEZONE=Europe/Paris
AELIA_OPENMETEO_API_KEY=

AELIA_WEATHERAPI_ENABLED=true
AELIA_WEATHERAPI_API_KEY=

AELIA_VISUALCROSSING_ENABLED=true
AELIA_VISUALCROSSING_API_KEY=

AELIA_OPENWEATHER_ENABLED=true
AELIA_OPENWEATHER_API_KEY=

AELIA_PIRATEWEATHER_ENABLED=true
AELIA_PIRATEWEATHER_API_KEY=

AELIA_WEATHERBIT_ENABLED=true
AELIA_WEATHERBIT_API_KEY=

AELIA_METNORWAY_ENABLED=false

AELIA_OPENMETEO_CONNECT_TIMEOUT_SECONDS=20
AELIA_OPENMETEO_REQUEST_TIMEOUT_SECONDS=40
AELIA_OPENMETEO_ARCHIVE_FALLBACK_LAG_DAYS=2
AELIA_DIAGNOSTICS=true
```

## API key creation links

- Open-Meteo commercial customer key: https://open-meteo.com/en/pricing
- WeatherAPI.com: https://www.weatherapi.com/signup.aspx
- Visual Crossing: https://www.visualcrossing.com/sign-up/
- OpenWeather: https://home.openweathermap.org/users/sign_up
- Pirate Weather: https://pirate-weather.apiable.io/products/weather-data-api
- Weatherbit: https://www.weatherbit.io/account/create

## Security notes

- Do not commit `.env`.
- Use `.env.example` only for empty placeholders and documentation.
- Keep provider keys out of screenshots, issue reports and logs.
- Prefer low refresh frequencies and cache reuse to avoid hitting free-tier quotas.
